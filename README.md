# TecoTerm

Terminal y administrador de servidores Linux para Android, por SSH.

> El servidor ejecuta el trabajo; TecoTerm le da una interfaz móvil profesional para administrarlo.

TecoTerm no ejecuta Linux en el teléfono (no es Termux). Se conecta a servidores remotos y combina una terminal SSH real con herramientas visuales: archivos, Docker, monitoreo, servicios y logs.

**Versión:** 0.1.0 — **Estado:** diseño

---

## 1. Regla fundamental: core propio

TecoTerm tiene su propio motor SSH y su propio emulador de terminal. No se usan librerías de terceros para SSH, SFTP ni terminal (nada de Apache MINA, sshj, JSch, Termux terminal-emulator, termlib ni similares).

Única excepción: las **primitivas criptográficas** (AES, ChaCha20-Poly1305, X25519, Ed25519, RSA, SHA-2, HMAC, generador aleatorio) se toman de la plataforma Android (JCA / Conscrypt). Nunca se implementan a mano.

## 2. Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Estado | ViewModel, StateFlow, Coroutines |
| Navegación | Navigation Compose |
| Base local | Room |
| Secretos | Android Keystore |
| Cripto | JCA / Conscrypt de la plataforma |
| SSH / SFTP | Propio (`core-ssh`) |
| Terminal | Propio (`core-terminal`) |
| Backend | Ninguno en el MVP: conexión directa teléfono → servidor |

`minSdk` 33 (Android 13) como punto de partida, por disponibilidad de X25519/Ed25519 en la plataforma. Se confirma en la fase 1 qué algoritmos expone realmente; si alguno falta, el respaldo es `ecdh-sha2-nistp256` / `ecdsa-sha2-nistp256`.

## 3. Módulos Gradle

```text
tecoterm/
├── app/              Compose, navegación, ViewModels, inyección
├── core-ssh/         Protocolo SSH: transporte, kex, auth, canales, SFTP, forwarding
├── core-terminal/    Emulador: parser VT/xterm, buffer de celdas, scrollback
├── core-security/    Keystore, cifrado de claves, known_hosts, biometría, PIN
├── data/             Room, repositorios, modelos
└── feature-*/        servers, terminal, files, docker, monitor, services, logs
```

Reglas de dependencia:

- `core-ssh` y `core-terminal` son Kotlin puro (sin Android), probables en la JVM.
- `core-terminal` no conoce SSH: recibe bytes y produce un estado de pantalla.
- Las `feature-*` no tocan sockets: hablan con el `SessionManager`.

## 4. Core SSH (`core-ssh`)

Implementación de SSH-2 según RFC 4251–4254 y sus extensiones.

**Transporte (RFC 4253)**

- TCP con timeout, intercambio de versión `SSH-2.0-TecoTerm_x.y`.
- Lectura y escritura de paquetes binarios: longitud, relleno, MAC / AEAD, número de secuencia.
- Negociación `KEXINIT`, re-key por volumen y por tiempo.
- Kex: `curve25519-sha256`, respaldo `ecdh-sha2-nistp256`. Extensión `ext-info-c` y kex estricto (mitigación Terrapin).
- Cifrados: `chacha20-poly1305@openssh.com`, `aes256-gcm@openssh.com`, `aes128-gcm@openssh.com`.
- Host key: `ssh-ed25519`, `rsa-sha2-512`, `rsa-sha2-256`, `ecdsa-sha2-nistp256`.

**Autenticación (RFC 4252)**

- `publickey` (Ed25519, RSA con SHA-2, ECDSA).
- `password`.
- `keyboard-interactive` (2FA / OTP).

**Conexión (RFC 4254)**

- Multiplexación de canales con ventana y control de flujo.
- `pty-req`, `shell`, `exec`, `window-change`, señales, `exit-status`.
- Keepalive y detección de conexión muerta.

**Después del MVP**

- SFTP v3 (draft-ietf-secsh-filexfer-02) sobre un canal `subsystem`.
- Port forwarding local y remoto (`direct-tcpip`, `tcpip-forward`).

**Claves**

- Lectura del formato `openssh-key-v1` (con y sin passphrase, bcrypt-pbkdf propio sobre SHA-512 de plataforma), PEM PKCS#1/PKCS#8.
- Generación de claves Ed25519 en el teléfono y exportación de la pública.

## 5. Core terminal (`core-terminal`)

- Parser de estados VT500 (ground, escape, CSI, OSC, DCS) con UTF-8 incremental.
- Buffer de celdas: carácter, ancho (CJK / emojis de ancho doble), atributos (negrita, cursiva, subrayado, inverso), color 16 / 256 / 24 bits.
- Pantalla principal con scrollback y pantalla alternativa (vim, htop, less).
- Regiones de scroll, inserción y borrado de líneas y caracteres, tabulaciones.
- Modos: cursor de aplicación, bracketed paste, autowrap, origen, mouse (fase posterior).
- Respuestas al servidor: reporte de posición del cursor, atributos del dispositivo.
- Resize con reflujo.
- `TERM=xterm-256color`.
- Render propio en `Canvas` de Compose, solo de las líneas que cambiaron.
- Entrada: IME, barra de teclas (ESC, TAB, CTRL, ALT, flechas, `/ | ~ -`), selección y copia.

Criterio de éxito del emulador: `bash`, `vim`, `nano`, `htop`, `top`, `less`, `git log`, `docker logs -f`, `python` y `node` interactivos se ven y responden igual que en una terminal de escritorio.

## 6. Seguridad

- Las claves privadas nunca se guardan en texto plano. Se cifran con AES-256-GCM usando una clave que vive en el Android Keystore (no exportable); Room solo guarda una referencia (`keyId`).
- Las contraseñas de servidor siguen el mismo esquema.
- `known_hosts` propio: la primera conexión muestra la huella SHA-256 y el usuario decide; si la clave cambia, se bloquea la conexión con advertencia de posible MITM.
- Desbloqueo con biometría o PIN; bloqueo automático por inactividad.
- Cierre seguro de sesiones y borrado de material de clave en memoria al desconectar.

## 7. Datos locales (Room)

```text
Server        id, name, host, port, username, authType, credentialId, createdAt, lastConnected
Credential    id, kind (key | password), label, publicKey, keystoreAlias, encryptedBlob
KnownHost     host, port, keyType, fingerprint, firstSeen
Snippet       id, label, command, serverId?
```

## 8. Sesiones y conexión

- Varias sesiones simultáneas, cada una con su conexión, terminal, tamaño y estado.
- Servicio en primer plano solo mientras haya sesiones abiertas; sin conexiones en segundo plano cuando no hay ninguna.
- Pérdida de red, cambio Wi-Fi ↔ datos, timeout, servidor caído, clave rechazada, host desconocido y cierre remoto se muestran con un estado claro y las acciones Reintentar / Desconectar.
- Reconexión con opción de reabrir `tmux` para recuperar el trabajo.

## 9. Módulos visuales

Nunca leen la salida de la terminal. Cada uno abre su propio canal `exec` y pide formatos parseables:

| Módulo | Fuente |
|---|---|
| Docker | `docker ps -a --format '{{json .}}'`, `docker inspect`, `docker logs` |
| Monitor | `/proc/stat` (dos muestras), `/proc/meminfo`, `df -P`, `/proc/net/dev`, `/proc/uptime` |
| Servicios | `systemctl list-units --output=json`, `systemctl show -p ...` |
| Logs | `journalctl -o json`, `docker logs`, archivos con `tail` |

Usuarios sin root: los comandos que requieren `sudo` piden la contraseña en la app y la envían por el canal; nunca se guarda en el comando.

Monitoreo con intervalo configurable: 5 s, 10 s, 30 s, 60 s o desactivado.

## 10. Operaciones peligrosas

Las acciones lanzadas desde botones (detener o reiniciar servicios, `docker stop`, `prune`, borrar archivos, `reboot`, `shutdown`) piden confirmación indicando qué se va a afectar.

Lo que el usuario escribe en la terminal nunca se modifica ni se intercepta.

## 11. Interfaz

- Mobile first.
- Sin emojis. Los estados se muestran con íconos y puntos de color.
- La UI responde de inmediato aunque el servidor tarde: estados de carga explícitos, nunca bloqueo.

## 12. Fases

```text
FASE 1  Proyecto, módulos Gradle, servidor OpenSSH de pruebas en Docker
FASE 2  core-ssh: transporte, kex, cifrado, host key, auth publickey/password, canal shell
FASE 3  core-security: Keystore, credenciales, known_hosts
FASE 4  Gestión de servidores (Room + UI)
FASE 5  core-terminal: parser, buffer, render, teclado
FASE 6  Múltiples sesiones, servicio en primer plano, reconexión
FASE 7  SFTP y explorador de archivos
FASE 8  Docker
FASE 9  Monitoreo
FASE 10 Servicios y logs
FASE 11 Port forwarding, snippets, biometría / PIN
FASE 12 Optimización, beta, publicación
```

No se avanza de fase hasta que la anterior esté estable.

## 13. Pruebas

- `core-ssh` se prueba en la JVM contra un OpenSSH real en Docker (`infra/sshd-pruebas`), con cada algoritmo soportado y casos de fallo (clave errónea, host key cambiada, corte de red, re-key).
- `core-terminal` se prueba con secuencias grabadas de programas reales comparando el estado de pantalla esperado.

## 14. Criterio de éxito del MVP

1. Instalar TecoTerm.
2. Registrar un servidor (host, puerto, usuario).
3. Importar o generar una clave SSH.
4. Aceptar la huella del servidor.
5. Conectarse y abrir la terminal.
6. Ejecutar `whoami`, `pwd`, `ls`, `cd`, `docker ps`, `vim`, `htop` con salida en tiempo real.
7. Desconectarse y reconectarse.
