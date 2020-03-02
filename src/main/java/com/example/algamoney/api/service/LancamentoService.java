package com.example.algamoney.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.algamoney.api.dto.LancamentoEstatisticaPessoa;
import com.example.algamoney.api.mail.Mailer;
import com.example.algamoney.api.model.Lancamento;
import com.example.algamoney.api.model.Pessoa;
import com.example.algamoney.api.model.Usuario;
import com.example.algamoney.api.repository.LancamentoRepository;
import com.example.algamoney.api.repository.PessoaRepository;
import com.example.algamoney.api.repository.UsuarioRepository;
import com.example.algamoney.api.service.exception.PessoaInexistenteOuInativaException;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class LancamentoService {
	
	private static final String DESTINATARIOS = "ROLE_PESQUISAR_LANCAMENTO";
	
	private static final Logger logger = LoggerFactory.getLogger(Lancamento.class);

	@Autowired
	private PessoaRepository pessoaRepository;
	
	@Autowired
	private LancamentoRepository lancamentoRepository;
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	@Autowired
	private Mailer mailer;
	
	@Value("${files.dir}")
	private String localArquivos;
	
	public String uploadAnexo(MultipartFile anexo) throws IOException {
		Path fileStorageLocation = Paths.get(localArquivos).toAbsolutePath().normalize();
		if (!Files.exists(fileStorageLocation)) {
			Files.createDirectories(fileStorageLocation);
		}
		String fileName = anexo.getOriginalFilename();
        Path targetLocation = fileStorageLocation.resolve(fileName);
        Files.copy(anexo.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);		
		return targetLocation.toString();
	}
	
	@Scheduled(cron = "0 0 6 * * *")
	public void avisarSobreLancamentosVencidos() {
		if (logger.isDebugEnabled()) {
			logger.debug("Preparando envio de emails de aviso de lançamentos vencidos.");
		}
		
		List<Lancamento> vencidos = lancamentoRepository.findByDataVencimentoLessThanEqualAndDataPagamentoIsNull(LocalDate.now());
		if (vencidos.isEmpty()) {
			logger.info("Sem lançamentos para aviso.");
			return;
		}
		logger.info("Existem {} lançamentos vencidos.", vencidos.size());
		
		List<Usuario> destinatarios = usuarioRepository.findByPermissoesDescricao(DESTINATARIOS);
		
		if (destinatarios.isEmpty()) {
			logger.warn("Existem lançamentos vencidos, mas o sistema não encontrou destinários.");
			return;
		}
		
		mailer.avisarSobreLancamentosVencidos(vencidos, destinatarios);
		
		logger.info("Envio de e-mail de aviso concluído.");
		
	}
	
	public byte[] relatorioPorPessoa(LocalDate inicio, LocalDate fim) throws JRException {
		List<LancamentoEstatisticaPessoa> dados = lancamentoRepository.porPessoa(inicio, fim);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Map<String, Object> paramentros = new HashMap();
		paramentros.put("DT_INICIO", Date.valueOf(inicio));
		paramentros.put("DT_FIM", Date.valueOf(fim));
		paramentros.put("REPORT_LOCALE", new Locale("pt", "BR"));
		
		InputStream inputStream = this.getClass()
				.getResourceAsStream("/relatorios/lancamentos-por-pessoa.jasper");
		
		JasperPrint jasperPrint = JasperFillManager.fillReport(inputStream, paramentros, new JRBeanCollectionDataSource(dados));
		
		return JasperExportManager.exportReportToPdf(jasperPrint);
		
	}

	public Lancamento salvar(Lancamento lancamento) {
		Pessoa pessoa = pessoaRepository.findById(lancamento.getPessoa().getCodigo()).get();
		if (pessoa == null || pessoa.IsInativo()) {
			throw new PessoaInexistenteOuInativaException();
		}
		
		return lancamentoRepository.save(lancamento);
	}
	
	public Lancamento atualizar(Long codigo, Lancamento lancamento) {
		Lancamento lancamentoSalvo = buscarLancamentoExistente(codigo);
		if (!lancamento.getPessoa().equals(lancamentoSalvo.getPessoa())) {
			validarPessoa(lancamento);
		}
		BeanUtils.copyProperties(lancamento, lancamentoSalvo, "codigo");
		return lancamentoRepository.save(lancamentoSalvo);
	}
	
	private void validarPessoa(Lancamento lancamento) {
		Pessoa pessoa = pessoaRepository.findById(lancamento.getPessoa().getCodigo()).get();
		if (pessoa == null || pessoa.IsInativo()) {
			throw new PessoaInexistenteOuInativaException();
		}
	}
	
	private Lancamento buscarLancamentoExistente(Long codigo) {
		Lancamento lancamentoSalvo = lancamentoRepository.findById(codigo).get();
		if (lancamentoSalvo == null) {
			throw new IllegalArgumentException();
		}
		return lancamentoSalvo;
	}
	
	
}
