# TecoTerm

Terminal y administrador de servidores Linux para Android, por SSH.

TecoTerm no ejecuta Linux en el teléfono. Se conecta a servidores remotos y combina una terminal SSH real con herramientas visuales: archivos, Docker, monitoreo, servicios y logs.

Versión 0.1.0, en desarrollo.

## 1. Core propio

El motor SSH y el emulador de terminal son propios. No se usan librerías de terceros para SSH, SFTP ni terminal.

Las primitivas criptográficas (AES, X25519, Ed25519, RSA, ECDSA, SHA-2, HMAC y el generador aleatorio) son las de la plataforma Android y no se implementan a mano.

## 2. Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose, Material 3 |
| Estado | ViewModel, StateFlow, Coroutines |
| Navegación | Navigation Compose |
| Base local | Room |
| Secretos | Android Keystore |
| Criptografía | JCA / Conscrypt |
| SSH y SFTP | Propio |
| Terminal | Propio |
| Backend | Ninguno: el teléfono se conecta directo al servidor |

Android 13 o superior.

## 3. Motor SSH

Implementa SSH-2 según los RFC 4251 a 4254.

### Transporte

- Intercambio de versión, paquetes binarios, relleno, MAC y números de secuencia.
- Negociación de algoritmos, re-key por volumen y por tiempo.
- Intercambio de claves `curve25519-sha256` y `ecdh-sha2-nistp256`, con `ext-info-c` y kex estricto contra Terrapin.
- Cifrado `aes256-gcm@openssh.com`, `aes128-gcm@openssh.com`, `aes256-ctr` y `aes128-ctr`.
- MAC `hmac-sha2-256-etm@openssh.com`, `hmac-sha2-512-etm@openssh.com`, `hmac-sha2-256` y `hmac-sha2-512` para los cifrados sin AEAD.
- Host key `ssh-ed25519`, `rsa-sha2-512`, `rsa-sha2-256` y `ecdsa-sha2-nistp256`.
- Solo se ofrecen al servidor los algoritmos que el teléfono soporta.

### Autenticación

- Clave pública: Ed25519, RSA con SHA-2 y ECDSA.
- Contraseña.
- Keyboard-interactive para códigos de verificación.

### Conexión

- Canales multiplexados con ventana y control de flujo.
- Terminal remota, shell, ejecución de comandos, cambio de tamaño, señales y código de salida.
- Keepalive y detección de conexión muerta.
- Más adelante: SFTP versión 3 y redirección de puertos local y remota.

### Claves

- Importación de claves OpenSSH, con y sin passphrase, y PEM.
- Generación de claves Ed25519 en el teléfono y exportación de la pública.

## 4. Emulador de terminal

- Secuencias VT100 y xterm: escape, CSI, OSC y DCS, con UTF-8.
- Colores de 16, 256 y 24 bits, negrita, cursiva, subrayado e inverso.
- Caracteres de ancho doble.
- Historial y pantalla alternativa para vim, htop y less.
- Regiones de scroll, inserción y borrado, tabulaciones.
- Cursor de aplicación, pegado entre corchetes, ajuste de línea y, más adelante, mouse.
- Reflujo al cambiar el tamaño.
- Dibujo propio en Compose, solo de las líneas que cambian.
- Teclado con barra de teclas especiales (ESC, TAB, CTRL, ALT, flechas, `/ | ~ -`), selección y copia.

Programas que deben verse y responder igual que en una terminal de escritorio: `bash`, `vim`, `nano`, `htop`, `top`, `less`, `git log`, `docker logs -f`, `python` y `node`.

## 5. Seguridad

- Las claves privadas y contraseñas se guardan cifradas con AES-256-GCM, con una clave del Android Keystore que no sale del teléfono.
- La primera conexión a un servidor muestra la huella SHA-256 de su clave y el usuario decide si la acepta.
- Si la clave de un servidor conocido cambia, la conexión se bloquea con aviso de posible ataque.
- Desbloqueo con biometría o PIN y bloqueo automático por inactividad.
- Al desconectar se cierra la sesión y se borra de memoria el material de clave.

## 6. Datos

| Registro | Datos |
|---|---|
| Servidor | Nombre, host, puerto, usuario, tipo de autenticación, credencial, fecha de registro, última conexión |
| Credencial | Tipo (clave o contraseña), nombre, clave pública, referencia en el Keystore, contenido cifrado |
| Servidor conocido | Host, puerto, tipo de clave, huella, primera vez visto |
| Comando guardado | Nombre, comando, servidor opcional |

## 7. Sesiones

- Varias sesiones a la vez, cada una con su conexión, terminal, tamaño y estado.
- La app mantiene las sesiones abiertas en segundo plano solo mientras existan.
- Pérdida de red, cambio de Wi-Fi a datos, timeout, servidor caído, clave rechazada, host desconocido y cierre remoto se muestran con su estado y las opciones Reintentar y Desconectar.
- Al reconectar se puede volver a la sesión de `tmux`.

## 8. Herramientas visuales

Cada herramienta abre su propio canal de ejecución, nunca lee la terminal.

| Herramienta | Datos |
|---|---|
| Docker | Contenedores, estado, inicio, detención, reinicio, logs, inspección |
| Monitoreo | CPU, RAM, disco, red, uptime, procesos |
| Servicios | Estado, inicio, detención, reinicio |
| Logs | Sistema, Docker, archivos |

- Con usuarios sin root, la contraseña de `sudo` se pide en la app y nunca queda escrita en el comando.
- El monitoreo se actualiza cada 5, 10, 30 o 60 segundos, o se desactiva.
- Detener, reiniciar, borrar o apagar desde un botón pide confirmación y dice qué se afecta.
- Lo que el usuario escribe en la terminal nunca se modifica.

## 9. Interfaz

- Pensada para teléfono.
- Sin emojis: los estados se muestran con íconos y puntos de color.
- Responde al instante aunque el servidor tarde.

## 10. Fases

| Fase | Contenido |
|---|---|
| 1 | Proyecto, análisis de código, integración continua, servidor SSH de pruebas |
| 2 | Motor SSH: transporte, intercambio de claves, cifrado, host key, autenticación, shell |
| 3 | Seguridad: Keystore, credenciales, servidores conocidos |
| 4 | Gestión de servidores |
| 5 | Emulador de terminal |
| 6 | Varias sesiones, segundo plano, reconexión |
| 7 | SFTP y explorador de archivos |
| 8 | Docker |
| 9 | Monitoreo |
| 10 | Servicios y logs |
| 11 | Redirección de puertos, comandos guardados, biometría y PIN |
| 12 | Optimización, beta y publicación |

Cada fase empieza cuando la anterior está estable.

## 11. Pruebas

- El motor SSH se prueba contra un servidor OpenSSH real, con cada algoritmo y con fallos: clave errónea, host key cambiada, corte de red y re-key.
- El emulador se prueba con secuencias grabadas de programas reales contra la pantalla esperada.

## 12. MVP

1. Instalar TecoTerm.
2. Registrar un servidor.
3. Importar o generar una clave SSH.
4. Aceptar la huella del servidor.
5. Conectarse y abrir la terminal.
6. Ejecutar `whoami`, `pwd`, `ls`, `cd`, `docker ps`, `vim` y `htop` con salida en tiempo real.
7. Desconectarse y volver a conectarse.
