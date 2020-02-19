CREATE TABLE pessoa (
	codigo BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(50) NOT NULL,
    ativo TINYINT (1) NOT NULL,
    logradouro VARCHAR(80),
    numero VARCHAR(10),
    complemento VARCHAR(30),
    bairro VARCHAR(40),
    cep VARCHAR(15),
    cidade VARCHAR(50),
    estado VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO pessoa(nome, ativo, logradouro, numero, complemento, bairro, cep, cidade, estado)
VALUES('William Cassio', 1, 'Av. Desembargador João Machado', '1479', null, 'Planalto', '69.044-000', 'Manaus', 'Amazonas');
INSERT INTO pessoa(nome, ativo, logradouro, numero, complemento, bairro, cep, cidade, estado)
VALUES('Suzane Gomes', 1, 'Av. Desembargador João Machado', '1479', null, 'Planalto', '69.044-000', 'Manaus', 'Amazonas');
INSERT INTO pessoa(nome, ativo, logradouro, numero, complemento, bairro, cep, cidade, estado)
VALUES('Benício Oliveira', 1, null, null, null, null, null, null, null);