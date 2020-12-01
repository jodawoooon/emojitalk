# -*- coding: utf-8 -*-
"""
Created on Mon Sep 28 22:22:37 2020

@author: DaunJO
"""


from flask import Flask, render_template, request
from gevent.pywsgi import WSGIServer
from flask_caching import Cache
import os
import server3

app = Flask(__name__)

file_path = './static'
file_list = os.listdir(file_path)

for file_name in file_list :
    print(file_name)
    server3.deleteFile(file_name)


@app.route('/')
def home():
    
    pics = os.listdir('static/')
    
    
    return render_template('img_static.html', pics=pics)

#if __name__ == "__main__":
    #http_server = WSGIServer(('', 5000), app)

    #http_server.serve_forever()
