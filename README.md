#Trabalho Sistemas Distribuídos

Implementação do Protoloco DHT Simplificado usando o framework dado em sala



1. Espaço de Endereçamento Único (Hashing): Tanto os dados (chaves) quanto os servidores (nós) são mapeados para o mesmo espaço de identificadores numéricos usando uma função de hash (como SHA-1). A chave do dado vira um ID: $ID_{dado} = \text{hash}(\text{"foto.jpg"})$ O nó vira um ID: $ID_{no} = \text{hash}(\text{"192.168.1.50"})$


2. Atribuição de Responsabilidade: Uma regra matemática simples determina qual nó armazena qual chave. No modelo simplificado mais comum (anel lógico do Chord):A chave é armazenada no primeiro nó cujo ID seja maior ou igual ao ID da chave (chamado de nó sucessor).


3. Roteamento (Busca): Em uma DHT real, cada nó conhece apenas alguns vizinhos (tabelas de roteamento como a Finger Table do Chord) para garantir buscas em tempo logarítmico ($O(\log N)$). Em uma DHT simplificada ao extremo:Cada nó conhece apenas o seu sucessor imediato na rede, formando um anel linear.Fluxo de busca: Se o Nó A procura pela chave 15, ele verifica se ele é o responsável. Se não for, ele passa a requisição para o seu sucessor, que faz o mesmo, repetindo o processo linearmente ($O(N)$) até encontrar o nó correto.

