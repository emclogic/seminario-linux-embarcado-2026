# Seminário Linux Embarcado 2026

Projeto demonstrativo do **Seminário Linux Embarcado 2026**, mostrando como construir uma imagem Linux embarcada com Yocto/OpenEmbedded capaz de executar containers OCI via Podman, emulada em QEMU ARM64.

## Visão Geral

Este projeto demonstra o fluxo completo de:

1. Construir uma **imagem host** (Linux embarcado) com suporte a containers via Podman
2. Construir uma **imagem de container OCI** contendo uma aplicação Flask
3. Emular o sistema com **QEMU ARM64**
4. Transferir e executar o container na imagem host emulada
5. Testar a aplicação web e realizar **atualizações** do container

### Stack de tecnologias

| Componente         | Tecnologia                              |
|--------------------|-----------------------------------------|
| Build system       | Yocto / OpenEmbedded (branch Walnascar) |
| Configuração build | Kas                                     |
| Distro base        | Poky                                    |
| Target machine     | QEMU ARM64                              |
| Runtime container  | Podman                                  |
| Formato container  | OCI                                     |
| Aplicação          | Flask (Python 3)                        |

---

## Estrutura do Projeto

```
seminario-linux-embarcado-2026/
├── conf/
│   └── layer.conf                          # Configuração da layer meta-sle-2026
├── kas/                                    # Arquivos de configuração do Kas
│   ├── base.yaml                           # Configuração base do OpenEmbedded
│   ├── walnascar.yaml                      # Repositórios e layers (branch Walnascar)
│   ├── machines/
│   │   └── qemuarm64.yaml                 # Definição da máquina QEMU ARM64
│   └── distro/
│       └── linux-embarcado-2026.yaml       # Configuração da distro customizada
├── recipes-demo/
│   ├── helloworld-embarcados/
│   │   ├── helloworld-embarcados.bb        # Recipe BitBake do app Flask
│   │   └── files/qemuall/files/
│   │       └── flask-app                   # Código-fonte da aplicação Flask
│   └── images/
│       ├── embarcados-host-image.bb        # Recipe da imagem host (com Podman)
│       └── app-embarcados-container.bb     # Recipe da imagem container OCI
├── host-image.yaml                         # Alvo de build: imagem host
├── container-app.yaml                      # Alvo de build: container da aplicação
├── run-qemu.sh                             # Script para iniciar o QEMU
└── README.md
```

### Layers utilizadas

| Layer                | Repositório                                          | Finalidade                             |
|----------------------|------------------------------------------------------|----------------------------------------|
| `meta` / `meta-poky` | git.yoctoproject.org/poky (branch walnascar)         | Core do Yocto                          |
| `meta-oe`            | github.com/openembedded/meta-openembedded            | Receitas extras OpenEmbedded           |
| `meta-python`        | github.com/openembedded/meta-openembedded            | Suporte a Python 3 e Flask             |
| `meta-networking`    | github.com/openembedded/meta-openembedded            | Suporte a rede                         |
| `meta-virtualization`| git.yoctoproject.org/meta-virtualization             | Podman e suporte a containers          |
| `meta-sle-2026`      | Este repositório                                     | Receitas e imagens do projeto          |

---

## Pré-requisitos

### Host Linux

- Git
- Docker ou Podman

> O `kas-container` executa o build dentro de um container com todas as dependências do Yocto já incluídas. **Não é necessário instalar Python, BitBake nem nenhuma dependência do Yocto na máquina host.**

---

## Configuração do kas-container

O [Kas](https://kas.readthedocs.io/) é uma ferramenta de configuração de builds Yocto que gerencia automaticamente o clone de repositórios e a configuração de layers.

Baixe o script `kas-container` diretamente do repositório oficial:

```bash
curl -fsSL https://raw.githubusercontent.com/siemens/kas/master/kas-container -o kas-container
chmod +x kas-container
```

---

## Build

### 1. Clonar o repositório

```bash
git clone <url-deste-repositorio> seminario-linux-embarcado-2026
cd seminario-linux-embarcado-2026
```

### 2. Construir a imagem host

A imagem host é um sistema Linux embarcado com Podman, capaz de executar containers OCI.

```bash
./kas-container build host-image.yaml
```

> Este passo faz o clone automático de todas as layers e executa o BitBake. Pode levar de **1 a 3 horas** dependendo do hardware e da velocidade de internet.

Artefatos gerados em: `build/tmp/deploy/images/qemuarm64/`

### 3. Construir a imagem do container

A imagem do container OCI contém a aplicação Flask.

```bash
./kas-container build container-app.yaml
```

Artefatos gerados em: `build/tmp/deploy/images/qemuarm64/app-embarcados-container-latest-oci.tar`

---

## Executando no QEMU

### Iniciar o emulador

```bash
./run-qemu.sh
```

O script:
1. Configura o ambiente de build do Yocto
2. Inicializa o `runqemu` com a imagem host
3. Emula uma máquina ARM64 com **1 GB de RAM** em modo **nographic** (terminal)

Após o boot, o QEMU estará disponível na rede com o IP `192.168.7.2` (rede SLIRP padrão do runqemu).

> Para sair do QEMU: `Ctrl+A` seguido de `X`

---

## Trabalhando com Containers no QEMU

### 1. Transferir a imagem do container para o QEMU

No terminal do seu computador (host), copie a imagem OCI para o sistema emulado via SCP:

```bash
scp build/tmp/deploy/images/qemuarm64/app-embarcados-container-latest-oci.tar root@192.168.7.2:/tmp/
```

> Senha: não é necessária (imagem configurada sem senha para root em modo desenvolvimento)

### 2. Montar cgroups v2 (dentro do QEMU)

No terminal do QEMU, monte o subsistema cgroups necessário para o Podman:

```bash
mount -t cgroup2 none /sys/fs/cgroup
```

### 3. Carregar a imagem no Podman (dentro do QEMU)

```bash
podman load -i /tmp/app-embarcados-container-latest-oci.tar
```

### 4. Executar o container (dentro do QEMU)

```bash
podman run -d -p 10000:9000 localhost/latest
```

| Flag | Descrição |
|------|-----------|
| `-d` | Executa em background (daemon) |
| `-p 10000:9000` | Mapeia porta 10000 do host para porta 9000 do container |

---

## Testando a Aplicação

A aplicação Flask expõe três endpoints. Teste-os a partir do terminal do seu computador host:

```bash
curl http://192.168.7.2:10000/embarcados/
# Hello from Embarcados!

curl http://192.168.7.2:10000/emclogic/
# Hello from Emc Logic!

curl http://192.168.7.2:10000/linux/
# Hello from Seminario Linux Embarcado 2026!
```

---

## Atualizando a Aplicação

Para demonstrar o ciclo de atualização de um container embarcado:

### 1. Remover a imagem atual no QEMU

```bash
# Dentro do QEMU
podman stop $(podman ps -q)
podman rmi localhost/latest
```

### 2. Alterar o código da aplicação

Edite o arquivo [recipes-demo/helloworld-embarcados/files/qemuall/files/flask-app](recipes-demo/helloworld-embarcados/files/qemuall/files/flask-app) no seu computador host.

Exemplo de alteração — adicione `V2` nas mensagens de retorno:

```python
@app.route('/embarcados/', methods=['GET', 'POST'])
def welcome():
    return "Hello from Embarcados! V2\n"
```

### 3. Recompilar o container

```bash
./kas-container build container-app.yaml
```

### 4. Transferir a nova imagem

```bash
scp build/tmp/deploy/images/qemuarm64/app-embarcados-container-latest-oci.tar root@192.168.7.2:/tmp/
```

### 5. Carregar e executar a nova versão

```bash
# Dentro do QEMU
podman load -i /tmp/app-embarcados-container-latest-oci.tar
podman run -d -p 10000:9000 localhost/latest
```

### 6. Verificar a atualização

```bash
curl http://192.168.7.2:10000/embarcados/
# Hello from Embarcados! V2
```

---

## Detalhes Técnicos

### Aplicação Flask (`flask-app`)

A aplicação é um servidor HTTP minimalista escrito em Python 3 com Flask, escutando na porta `9000`:

```
GET /embarcados/ → "Hello from Embarcados!"
GET /emclogic/   → "Hello from Emc Logic!"
GET /linux/      → "Hello from Seminario Linux Embarcado 2026!"
```

### Imagem host (`embarcados-host-image`)

- Herda de `container-image-host.bb` (meta-virtualization)
- Inclui Podman como runtime de containers
- Inclui `curl` para testes de rede

### Imagem container OCI (`app-embarcados-container`)

- Imagem mínima com `base-files`, `base-passwd`, `netbase` e `busybox`
- Entrypoint: `/usr/bin/flask-app`
- Formato de saída: `container` + `oci`
- Sem packages recomendados (`NO_RECOMMENDATIONS = "1"`) para manter o tamanho mínimo

### Configuração da distro

A distro customizada `linux-embarcado-2026` habilita:

```
DISTRO_FEATURES: virtualization systemd seccomp usrmerge pam
IMAGE_FEATURES: allow-root-login allow-empty-password empty-root-password
```

> `allow-empty-password` e `empty-root-password` são exclusivos para ambiente de desenvolvimento/seminário. **Não use em produção.**

---

## Referências

- [Documentação do Yocto Project](https://docs.yoctoproject.org/)
- [Kas — Yocto Configuration Tool](https://kas.readthedocs.io/)
- [meta-virtualization](https://git.yoctoproject.org/meta-virtualization)
- [Podman Documentation](https://docs.podman.io/)
- [Flask Documentation](https://flask.palletsprojects.com/)
- [Emc Logic](https://www.emc-logic.com)
- [Embarcados.com.br](https://embarcados.com.br)

## Cursos

- [Yocto Project](https://cursos.embarcados.com.br/cursos/yocto-project-5-construcao-de-sistemas/)
- [Linux Embarcado Profissional](https://cursos.embarcados.com.br/cursos/linux-embarcado-profissional/)