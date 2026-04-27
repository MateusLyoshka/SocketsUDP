# Protocolo de Comunicacao - Upload UDP (Atividade 2)

Este documento especifica textualmente o protocolo usado pelos arquivos [Client.java](Client.java), [Server.java](Server.java) e [Payload.java](Payload.java) na pasta a2.

## Objetivo

Realizar upload de arquivo via UDP em tres fases:
1. START: anuncia metadados do arquivo.
2. DATA: envia conteudo em blocos de ate 1024 bytes.
3. END: envia checksum SHA-1 para validacao final.

## Convencoes Gerais

- Transporte: UDP.
- Ordem de bytes em campos numericos: big-endian (padrao de ByteBuffer no Java).
- Tamanho maximo de bloco de dados por pacote DATA: 1024 bytes.
- O servidor aceita um upload ativo por vez (estado unico no servidor atual).
- Pasta padrao de armazenamento no servidor: uploads.

## Tipos de Pacote

### 1) START (tipo = 1)

Funcao: iniciar upload, informando nome e tamanho do arquivo.

Formato:
- tipo: 1 byte
- tam_nome: 1 byte sem sinal (1..255)
- nome_arquivo: tam_nome bytes (UTF-8)
- tamanho_arquivo: 8 bytes (long)

Tamanho total:
- 1 + 1 + tam_nome + 8 bytes

Validacoes:
- nome nao vazio
- nome em UTF-8 com no maximo 255 bytes
- tamanho_arquivo >= 0

### 2) DATA (tipo = 2)

Funcao: enviar um bloco do arquivo.

Formato:
- tipo: 1 byte
- sequencia: 4 bytes (int)
- tam_dados: 2 bytes sem sinal (0..1024)
- dados: tam_dados bytes

Tamanho total:
- 1 + 4 + 2 + tam_dados bytes

Validacoes:
- sequencia >= 0
- tam_dados <= 1024
- tam_dados coerente com o payload recebido

Regra de ordem:
- O servidor espera sequencia estritamente crescente a partir de 0.
- Se receber sequencia diferente da esperada, ignora o pacote.

### 3) END (tipo = 3)

Funcao: finalizar upload e enviar checksum para integridade.

Formato:
- tipo: 1 byte
- tam_checksum: 1 byte sem sinal (deve ser 40)
- checksum: tam_checksum bytes (string hexadecimal SHA-1 em UTF-8)

Tamanho total:
- 1 + 1 + 40 bytes

Validacoes:
- checksum deve ter 40 caracteres hexadecimais

## Fluxo de Comunicacao

1. Cliente envia START com nome e tamanho do arquivo.
2. Cliente envia N pacotes DATA com sequencia 0, 1, 2, ...
3. Cliente envia END com SHA-1 do arquivo completo original.
4. Servidor reconstrui os bytes recebidos e valida:
- tamanho recebido == tamanho anunciado no START
- SHA-1 calculado == SHA-1 recebido no END
5. Se tudo estiver correto, servidor salva em uploads/nome_arquivo.
6. Em caso de divergencia de tamanho ou checksum, upload e descartado.

## Observacoes de Implementacao Atual

- O servidor associa o upload ativo ao par IP:porta do remetente que enviou o START.
- Pacotes de outro remetente sem START ativo sao ignorados.
- O nome do arquivo e sanitizado para evitar caminho arbitrario:
- Apenas o nome base e usado na gravacao (sem diretorios).

## Exemplo Resumido

- START: tipo=1, nome=foto.jpg, tamanho=4096
- DATA seq 0: 1024 bytes
- DATA seq 1: 1024 bytes
- DATA seq 2: 1024 bytes
- DATA seq 3: 1024 bytes
- END: checksum SHA-1 do arquivo inteiro
