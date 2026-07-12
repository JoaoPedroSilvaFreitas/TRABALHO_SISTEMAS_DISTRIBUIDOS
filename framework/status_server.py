#!/usr/bin/env python3
"""
Servidor HTTP simples para expor a pasta dht_status/ (gerada pelos nós Java)
com CORS liberado, permitindo que o artifact de visualização do anel
busque o estado atual de cada nó via fetch() no navegador.

Uso: python3 status_server.py [porta]
Rode a partir da pasta onde os nós Java estão sendo executados
(a mesma de onde dht_status/ é criada), na porta padrão 9000.
"""
import http.server
import socketserver
import sys

PORTA = int(sys.argv[1]) if len(sys.argv) > 1 else 9000


class HandlerComCORS(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def log_message(self, format, *args):
        pass  # silencia logs de request para não poluir o terminal


with socketserver.TCPServer(("", PORTA), HandlerComCORS) as httpd:
    print(f"Servindo status da DHT em http://localhost:{PORTA}/dht_status/")
    httpd.serve_forever()
