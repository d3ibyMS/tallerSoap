# TRM Colombia — Consumo de un servicio web SOAP en Android

App en Kotlin (MVVM) que consulta la TRM al servicio SOAP de la Superintendencia Financiera
de Colombia, convierte USD a COP y guarda las últimas consultas para verlas sin conexión.

## Abrir y ejecutar
1. Android Studio → File > Open → seleccionar la carpeta `TRMColombia`.
2. Esperar el *Gradle Sync* (aceptar la descarga del SDK 34 si lo pide).
3. Ejecutar en emulador o celular con internet (Run ▶).
4. Pruebas unitarias: clic derecho en `app/src/test` → Run Tests.

## Orden sugerido para explicar el código
| # | Archivo | Qué se explica |
|---|---------|----------------|
| 1 | `AndroidManifest.xml` | Solo el permiso INTERNET; sin tráfico HTTP sin cifrar |
| 2 | `data/SoapClient.kt` | Sobre XML (Envelope/Header/Body), POST, SOAPAction, Content-Type, timeouts |
| 3 | `data/TrmXmlParser.kt` | XmlPullParser: Fault, success=false y respuesta correcta |
| 4 | `data/TrmRepository.kt` | Caché vs. servicio, Dispatchers.IO, manejo de errores de red |
| 5 | `ui/TrmViewModel.kt` | Corrutinas y estado (Loading / Success / Error) |
| 6 | `ui/MainActivity.kt` | Observa el estado; no conoce SOAP |
| 7 | `app/src/test` | Pruebas del analizador con XML de ejemplo (ok, Fault, sin dato) |

## Demostración en vivo
Pulsar **Ver mensajes SOAP** (al final de la pantalla) tras consultar: muestra el sobre XML
enviado y la respuesta real del servidor. Casos para probar: fecha válida, fecha futura
(bloqueada), modo avión, y repetir la misma fecha (sale de la caché).

## Si el servicio no responde como se espera
Todo lo que depende del WSDL está en el `companion object` de `SoapClient.kt`
(ENDPOINT, NAMESPACE, OPERATION, PARAM_DATE, SOAP_ACTION). Confirmarlo abriendo el WSDL
y probando antes con SoapUI/Postman, como indica la guía.
