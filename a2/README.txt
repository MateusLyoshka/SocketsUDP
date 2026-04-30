Sistemas de Sockets UDP - Atividade 2

Descricao
Implementacao de upload de arquivos via UDP com tres tipos de pacote: START para iniciar o envio, DATA para enviar os blocos do arquivo e END para validar o upload com checksum SHA-1.

Autores:
- Mateus Santos Fernandes
- Matheus Floriano Saito da Silva

Data de criacao
27/04/2026

Data de atualizacao
30/04/2026

Como compilar
Na raiz do projeto, execute:
javac a2/Payload.java a2/Client.java a2/Server.java

Como executar
Servidor:
java -cp a2 Server

Cliente:
java -cp a2 Client

O servidor solicita a porta local.
O cliente solicita o caminho do arquivo, o IP de destino e a porta de destino.

Bibliotecas usadas
As bibliotecas utilizadas sao padroes do Java:
java.net, java.nio, java.nio.file, java.security, java.io e javax.swing.

Exemplo de uso
1. Execute o servidor e informe a porta, por exemplo 5000.
2. Execute o cliente.
3. Informe o caminho de um arquivo, por exemplo a2/base/oi.txt.
4. Informe o IP e a porta do servidor, por exemplo 127.0.0.1 e 5000.
5. Informe o caminho e o arquivo a ser enviado (inicio em a2/).
6. O servidor grava o arquivo em a2/uploads/.
