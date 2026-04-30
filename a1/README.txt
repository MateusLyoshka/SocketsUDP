Sistemas de Sockets UDP - Atividade 1

Descricao
Implementacao de comunicacao UDP ponto a ponto com envio de mensagens classificadas em NORMAL, EMOJI, URL e ECHO.

Autores:
- Mateus Santos Fernandes
- Matheus Floriano Saito da Silva

Data de criacao
26/04/2026

Data de atualizacao
30/04/2026

Como compilar
Na raiz do projeto, execute:
javac a1/Payload.java a1/Client.java

Como executar
Execute o cliente com:
java -cp a1 Client

O programa solicita apelido, porta local, IP de destino e porta de destino.

Bibliotecas usadas
As bibliotecas utilizadas sao padroes do Java:
java.net, java.nio.charset, java.util, java.io e javax.swing.

Exemplo de uso
1. Execute dois clientes em terminais diferentes.
2. Em um deles, informe a porta local, por exemplo 5001.
3. No outro, informe uma porta local diferente, por exemplo 5002.
4. Use o IP local, por exemplo 127.0.0.1, como destino.
5. Escolha um tipo de mensagem e informe o texto.
6. As mensagens recebidas aparecerao no console com o tipo informado.