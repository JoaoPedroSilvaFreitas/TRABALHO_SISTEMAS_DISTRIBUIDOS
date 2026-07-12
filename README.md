# Trabalho Sistemas Distribuídos

Implementação de um protocolo DHT simplificado (estilo Chord) usando o framework de máquina de estados orientada a eventos dado em sala.

## Conceito da DHT

Uma DHT (Distributed Hash Table) espalha um dicionário chave→valor entre vários nós de uma rede, de forma que cada nó guarda só uma fatia dos dados, mas qualquer nó consegue encontrar qualquer chave.

1. **Espaço de endereçamento único (hashing)** — nós e chaves são mapeados para o mesmo espaço numérico `[0, 1024)` usando SHA-256 e redução módulo 1024.
2. **Anel lógico** — os IDs formam um círculo. Uma chave pertence ao primeiro nó cujo ID seja maior ou igual ao ID da chave (o **sucessor** da chave).
3. **Roteamento simplificado** — cada nó só conhece seu sucessor e predecessor imediatos (não uma finger table completa como no Chord real). Uma busca é repassada nó a nó até achar o responsável — O(N) no pior caso, não O(log N).

## Estrutura do projeto

```
framework/
├── src/framework/
│   ├── Entidade.java        # nó da rede: buffer de eventos + thread de trabalho + estado atual
│   ├── Estado.java          # classe base para os estados da máquina (acao/transicao)
│   ├── EstadoAnel.java      # ESTADO CONCRETO: toda a lógica do protocolo DHT vive aqui
│   ├── Evento.java          # "PDU" do protocolo: code + C1/C2/C3 (campos genéricos de texto)
│   ├── EventoThread.java    # thread que roda o loop de eventos da Entidade
│   ├── Msg.java             # encapsula sockets TCP (conectar, enviar, receber)
│   ├── GerenciadorConexoes.java  # pool de conexões de saída, reaproveita sockets abertos
│   ├── ServidorRedeDHT.java # servidor de rede: aceita conexões e desempacota mensagens
│   ├── SocketThread.java    # classe base genérica do framework (não usada em produção aqui)
│   ├── SocketThreadDHT.java # especialização alternativa (não usada; ServidorRedeDHT é o ativo)
│   ├── Timeout.java         # dispara um Evento(code=3) após N ms, uma única vez
│   └── Main.java            # ponto de entrada: sobe um nó, faz JOIN opcional, abre console
├── dht_ring_viz.html         # visualizador web do anel (posição dos nós, sucessor/predecessor, chaves)
├── status_server.py          # servidor HTTP local que expõe dht_status/*.json para o visualizador
└── dht.sh                    # script auxiliar: compilar, subir nós, status-server, visualizador
```

### Como as peças se encaixam

- `Entidade` é o "motor": mantém uma fila (`buffer`) de `Evento`s e uma thread (`EventoThread`) que fica em loop infinito processando essa fila, chamando `transicao()` no estado atual.
- `EstadoAnel` é o único estado concreto implementado — nele mora toda a lógica de JOIN, roteamento, armazenamento e estabilização do anel.
- `ServidorRedeDHT` escuta conexões TCP, lê mensagens de texto (`code,C1,C2,C3`) e as transforma em `Evento`s colocados no buffer da `Entidade`.
- `GerenciadorConexoes` mantém um cache de sockets de saída por destino, evitando reabrir conexão a cada mensagem.

### Eventos do protocolo (`Evento.code`)

| Code | Nome | Papel |
|---|---|---|
| 1 | JOIN_REQ | Novo nó pede entrada no anel via um nó já conhecido |
| 2 | JOIN_RESP | Resposta informando sucessor e predecessor do novo nó |
| 3 | TIMEOUT | Disparado periodicamente, aciona a rotina de estabilização |
| 4 / 5 | LOOKUP_REQ / RESP | Descobre qual nó é responsável por uma chave, sem mexer em dados |
| 6 | PUT | Armazena um par chave-valor no nó responsável |
| 7 / 8 | GET / GET_RESP | Busca o valor de uma chave e retorna ao solicitante |
| 9 | NOTIFY | Um nó avisa outro "acho que sou seu predecessor agora" |
| 10 / 11 | ASK_PREDECESSOR / RESP | Usado na estabilização: pergunta ao sucessor quem é o predecessor dele |
| 12 / 13 | TRANSFER_KEYS_REQ / TRANSFER_KEY | Solicita e envia ao novo nó as chaves que passaram a pertencer a ele após o JOIN |

Cada nó mantém `idSucessor`/`ipSucessor`/`portaSucessor` e `idPredecessor`/`ipPredecessor`/`portaPredecessor`. A estabilização roda a cada 5s (reagendando um novo `Timeout` a cada rodada) e corrige o anel caso ele tenha ficado inconsistente.

## Passo a passo para rodar

Os comandos abaixo usam `./dht.sh` (Linux/macOS/WSL/Git Bash). No Windows sem bash, use `dht.bat` com a mesma sintaxe (ex: `dht.bat compilar`, `dht.bat no 8080`) — precisa de Python instalado e no PATH para o `status-server`.

### 1. Compilar

```bash
cd framework
./dht.sh compilar
```

### 2. Subir o servidor de status (opcional, só para o visualizador)

Em um terminal:
```bash
./dht.sh status-server
```
Serve os arquivos `dht_status/*.json` (gravados pelos nós) em `http://localhost:9000`, com CORS liberado para o visualizador conseguir buscar via `fetch()`.

### 3. Subir nós

Cada nó é um processo Java independente, numa porta própria. Em terminais separados:

```bash
./dht.sh no 8080                    # primeiro nó, sozinho no anel
./dht.sh no 8081 127.0.0.1 8080     # segundo nó, entra via JOIN no 8080
./dht.sh no 8082 127.0.0.1 8080     # terceiro nó, também usa 8080 como bootstrap
```

O IP de bootstrap é sempre `127.0.0.1` para testes na mesma máquina — não é necessário nem correto usar `127.0.1.1` ou o hostname da máquina (veja "Problemas conhecidos" abaixo).

Cada nó abre um console interativo simples:
```
put foto.jpg dadosdaimagem
get foto.jpg
sair
```
O PUT/GET é roteado automaticamente pelo anel até o nó responsável pela chave, não importa em qual nó você digitou o comando.

### 4. Abrir o visualizador

```bash
./dht.sh visualizador
```
Abre `dht_ring_viz.html` no navegador. Ele faz uma busca em largura a partir da porta informada (padrão `8080`), seguindo os ponteiros de sucessor/predecessor de cada nó para descobrir automaticamente todo o anel — não é preciso listar cada porta manualmente. Mostra:
- O anel desenhado com a posição real de cada nó no espaço de IDs (0–1023).
- Setas indicando o sucessor de cada nó.
- Uma tabela com ID, endereço, sucessor, predecessor e as chaves armazenadas em cada nó.

Nós que pararam de atualizar seu status por mais de 12s (processo encerrado) somem automaticamente da visualização.

### 5. Encerrar

```bash
./dht.sh listar        # ver quais nós estão rodando
./dht.sh parar-tudo     # encerrar todos de uma vez
```
Ou `sair` / Ctrl+C em cada terminal individualmente.

## Problemas conhecidos / limitações desta implementação simplificada

- **Transferência sem confirmação**: durante o JOIN, o sucessor envia ao novo nó as chaves de `(predecessor, novo nó]`, mas esta versão simplificada não espera uma confirmação de recebimento antes de remover a cópia antiga. Uma falha de rede exatamente durante a migração pode causar perda de dados.
- **Sem finger table**: roteamento é O(N), cada nó só conhece o vizinho imediato — é a simplificação pretendida pelo enunciado, não um bug.
- **Sem detecção de falha de nó**: se um nó cair sem avisar, seu sucessor/predecessor só percebe via estabilização (a cada 5s), e não há lógica de fallback para saltar o nó morto além de reencontrar o novo sucessor pela cadeia.
- **IP fixo em `127.0.0.1`**: necessário para testes na mesma máquina, porque `InetAddress.getLocalHost()` pode resolver para outro endereço (ex: `127.0.1.1` no `/etc/hosts` do Ubuntu), fazendo os nós se identificarem de formas inconsistentes entre si.
