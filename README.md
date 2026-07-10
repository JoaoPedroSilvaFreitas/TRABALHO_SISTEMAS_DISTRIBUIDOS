#Trabalho Sistemas Distribuídos

Implementação do Protoloco DHT Simplificado usando o framework dado em sala



1. Espaço de Endereçamento Único (Hashing): Tanto os dados (chaves) quanto os servidores (nós) são mapeados para o mesmo espaço de identificadores numéricos usando uma função de hash (como SHA-1). A chave do dado vira um ID: $ID_{dado} = \text{hash}(\text{"foto.jpg"})$ O nó vira um ID: $ID_{no} = \text{hash}(\text{"192.168.1.50"})$


2. Atribuição de Responsabilidade: Uma regra matemática simples determina qual nó armazena qual chave. No modelo simplificado mais comum (anel lógico do Chord):A chave é armazenada no primeiro nó cujo ID seja maior ou igual ao ID da chave (chamado de nó sucessor).


3. Roteamento (Busca): Em uma DHT real, cada nó conhece apenas alguns vizinhos (tabelas de roteamento como a Finger Table do Chord) para garantir buscas em tempo logarítmico ($O(\log N)$). Em uma DHT simplificada ao extremo:Cada nó conhece apenas o seu sucessor imediato na rede, formando um anel linear.Fluxo de busca: Se o Nó A procura pela chave 15, ele verifica se ele é o responsável. Se não for, ele passa a requisição para o seu sucessor, que faz o mesmo, repetindo o processo linearmente ($O(N)$) até encontrar o nó correto.













1. Definição do Espaço de Chaves (Hashing)
Estabelecer a função de hash (ex: SHA-1 ou funções mais simples para fins educacionais) para gerar identificadores numéricos (IDs) para os nós (baseado no IP/Porta) e para os dados (chaves). O espaço circular de IDs é o núcleo de qualquer DHT.

2. Mapeamento do Protocolo na Classe Evento
A classe Evento atual limita o tráfego a três atributos de texto (C1, C2, C3). Isso exige a definição de um protocolo estrito de serialização.

Atribuição do code: Definir constantes para as chamadas da DHT (ex: 1 = JOIN, 2 = FIND_SUCCESSOR, 3 = NOTIFY, 4 = PUT, 5 = GET).

Padronização das Strings: Estabelecer o conteúdo exato de cada variável por operação. Para FIND_SUCCESSOR, a estrutura poderia ser: C1 = "IP:Porta de Origem", C2 = "ID procurado", C3 = "IP:Porta de retorno". Para requisições que exigem múltiplos parâmetros, os dados deverão ser concatenados em um único campo (ex: C3 com dados formatados em JSON ou separados por delimitadores).

3. Especialização da SocketThread
Criar uma classe concreta que herde de SocketThread e implemente o método desempacota().

A rotina deverá processar a string armazenada em tmp, separá-la, converter os dados pertinentes, instanciar o objeto Evento correspondente e inseri-lo no buffer utilizando ent.colocaEvento().

4. Estruturas de Roteamento e Armazenamento Local
Criar a classe principal do nó da rede que herde de Entidade.

Embutir nesta classe as propriedades estruturais da DHT:

ID do nó.

Apontadores para o nó Predecessor e Sucessor.

Tabela de roteamento (Finger Table no Chord ou k-buckets no Kademlia).

Dicionário de dados (HashMap<String, String>) para armazenar os pares chave-valor de responsabilidade daquele nó específico.

5. Lógica da Máquina de Estados (Estado)
Implementar as fases de operação da DHT derivando da classe base Estado.

Estados necessários: Bootstrapping (contatando um nó conhecido para entrar na rede), TransferindoDados (assumindo a responsabilidade por chaves específicas após a entrada) e Operacional (processando buscas).

No estado Operacional, o método transicao(Evento e) deve conter a lógica de roteamento: ao receber uma requisição de busca, verificar matematicamente se o ID recai sobre a jurisdição do nó atual; se sim, processa o pedido, se não, repassa o evento via Msg.envia() para o nó mais próximo identificado na tabela de roteamento.

6. Manutenção da Topologia da Rede (Timeout)
Utilizar a classe Timeout existente para gerar os sinais de manutenção.

A máquina de estados precisará interceptar o evento gerado (code = 3 conforme a classe Timeout.java) para engatilhar processos de estabilização (stabilize), como o disparo de pings periódicos aos nós vizinhos para garantir que o anel lógico não foi rompido por desconexões abruptas.