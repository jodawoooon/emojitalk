import socket
import select
import time
from threading import Thread
from time import sleep

import glob
import re
import pandas as pd
import datetime as dt
import seaborn as sns
import matplotlib.pyplot as plt
import time
from konlpy.tag import Kkma
from PIL import Image
from wordcloud import WordCloud
import ast
import numpy as np
import flaskServer
import string
import random
import os
import sys
import signal
import threading


name1 = random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters)
name2 = random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters)
name3 = random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters)
name4 = random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters)
#name5 = random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters)

result = []
check = True
msg_list = []
username = []

def deleteFile(n):
    os.remove('./static/'+n)


def read_kko_msg(r):
    
    global msg_list
    msg_list = r
    for i in range(len(msg_list)):
        msg_list[i] = msg_list[i].replace("[","").replace("]","").replace(" ","").replace("\n","")
        
    for i in msg_list:
        print(i)
    return msg_list

def apply_kko_regex(msg_list):

    kko_pattern = re.compile("([\S\s]+):([\S\s]+)<([\S\s]+)>/Timestamp\(seconds=([0-9]+)")

    emoji_pattern = re.compile("["u"\U0001F600-\U0001F64F"  # emoticons
                               u"\U0001F300-\U0001F5FF"  # symbols & pictographs
                               u"\U0001F680-\U0001F6FF"  # transport & map symbols
                               u"\U0001F1E0-\U0001F1FF"  # flags (iOS)
                               "]+", flags=re.UNICODE)

    kko_parse_result = list()
    cur_datetime= ""
    cur_date = ""
    cur_datetype = ""
    cur_time= ""
    
    for msg in msg_list:
        kko_pattern_result = kko_pattern.findall(msg)
        print(kko_pattern_result)
        if len(kko_pattern_result) > 0:
            tokens = list(kko_pattern_result[0])
            cur_datetime = dt.datetime.strptime(convert_datetime(tokens[-1]), "%Y-%m-%d %H:%M:%S" )
            cur_date = cur_datetime.strftime("%Y-%m-%d")
            cur_datetype = cur_datetime.strftime("%p")
            cur_time = cur_datetime.strftime("%I:%M:%S")
            tokens.insert(0, cur_date)
            
            tokens.insert(2, cur_datetype)
            tokens.insert(3, cur_time)
            tokens.pop(6)
            print(tokens)
            #tokens[1] = tokens[1][1:]
            for i in tokens:
                
                if tokens[1] not in username:
                    
                    username.append(tokens[1])
            
            kko_parse_result.append(tokens)
        
    kko_parse_result = pd.DataFrame(kko_parse_result, columns=["Date", "Speaker", "timetype", "time", "contents", "emotions"])
    kko_parse_result.to_csv("./result/kko_regex.csv", index=False)

    return kko_parse_result

def convert_datetime(unixtime):
    """Convert unixtime to datetime"""
    import datetime
    date = datetime.datetime.fromtimestamp(int(unixtime)).strftime('%Y-%m-%d %H:%M:%S')
    return date # format : str



def get_noun(msg_txt):
    kkma = Kkma()
    nouns = list()
    pattern = re.compile("[ㄱ-ㅎㅏ-ㅣ]+")
    msg_txt = re.sub(pattern, "", msg_txt).strip()

    if len(msg_txt) > 0:
        pos = kkma.pos(msg_txt)
        for keyword, type in pos:
            # 고유명사 또는 보통명사
            if type == "NNG" or type == "NNP":
                nouns.append(keyword)
        #print(msg_txt, "->", nouns)

    return nouns


def get_all_token(msg_txt):
    kkma = Kkma()
    nouns = list()
    pattern = re.compile("[ㄱ-ㅎㅏ-ㅣ]+")
    msg_txt = re.sub(pattern, "", msg_txt).strip()

    if len(msg_txt) > 0:
        pos = kkma.pos(msg_txt)
        for keyword, type in pos:
            nouns.append(keyword)
        #print(msg_txt, "->", nouns)

    return nouns

def get_except_keyword(filename):
    keyword_list = list()
    with open(filename, encoding='utf-8') as f:
        for keyword in f.readlines():
            keyword_list.append(keyword.strip())
    print(keyword_list)
    return keyword_list


def draw_wordcloud(kkma_result):
    # List로 되어있는 열을 Row 단위로 분리
    tokens = pd.DataFrame(kkma_result["token"].apply(lambda x: ast.literal_eval(x)).tolist())

    tokens["Date"] = kkma_result["Date"]
    tokens["Speaker"] = kkma_result["Speaker"]
    tokens["timetype"] = kkma_result["timetype"]
    tokens["time"] = kkma_result["time"]
    tokens["contents"] = kkma_result["contents"]
    tokens["emotions"] = kkma_result["emotions"]
    
    tokens = tokens.set_index(["Date", "Speaker", "timetype", "time", "contents", "emotions"])
    tokens = tokens.T.unstack().dropna().reset_index()

    tokens.columns = ["Date", "Person", "time_type", "time", "sntc", "emotions", "index", "token"]
    print(tokens.head())

    # 빈도수 집계
    summary = tokens.groupby(["token"])["index"].count().reset_index()
    summary = summary.sort_values(["index"], ascending=[False]).reset_index(drop=True)

    # 특정 단어 필터링
    # except_keyword = get_except_keyword("./raw_data/except_word.txt")
    # summary = summary[summary["token"].apply(lambda x: x not in except_keyword)]
    summary = summary[summary["token"].apply(lambda x: len(x) > 1)]

    # 이미지 Mask 생성
    #denne_mask = np.array(Image.open("./font/denne.png"))

    # 워드클라우드 생성
    wc = WordCloud(font_path='./font/NanumGothic.ttf', background_color='white', width=800, height=600).generate(" ".join(summary["token"]))
    plt.imshow(wc)
    plt.axis("off")
    plt.show()
    plt.savefig('./static/'+name1+'.png')
    # plt.savefig('./static/wordcloud.png')

class SocketServer(Thread):
    
    def __init__(self, host = '0.0.0.0', port = 5000, max_clients = 10):
        """ Initialize the server with a host and port to listen to.
        Provide a list of functions that will be used when receiving specific data """
        Thread.__init__(self)
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.host = host
        self.port = port
        self.sock.bind((host, port))
        self.sock.listen(max_clients)
        self.sock_threads = []
        self.counter = 0 # Will be used to give a number to each thread

    def close(self):
        """ Close the client socket threads and server socket if they exists. """
        
        print('Closing server socket (host {}, port {})'.format(self.host, self.port))

        for thr in self.sock_threads:
            thr.stop()
            thr.join()

        if self.sock:
            self.sock.close()
            self.sock = None

    def run(self):
        """ Accept an incoming connection.
        Start a new SocketServerThread that will handle the communication. """
        print('Starting socket server (host {}, port {})'.format(self.host, self.port))
    
        self.__stop = False
        while not self.__stop:
            self.sock.settimeout(1)
            try:
                client_sock, client_addr = self.sock.accept()
            except socket.timeout:
                client_sock = None

            if client_sock:
                client_thr = SocketServerThread(client_sock, client_addr, self.counter)
                self.counter += 1
                self.sock_threads.append(client_thr)
                client_thr.start()             
        
        self.close()

    def stop(self):
        #print(result)
        read_kko_msg(result)
        self.__stop = True

class SocketServerThread(Thread):
    def __init__(self, client_sock, client_addr, number):
        """ Initialize the Thread with a client socket and address """
        Thread.__init__(self)
        self.client_sock = client_sock
        self.client_addr = client_addr
        self.number = number
        

        
    def run(self):
        #f = open('D:/cap/result/result.txt', 'w', encoding='utf-8')
        print("[Thr {}] SocketServerThread starting with client {}".format(self.number, self.client_addr))
        self.__stop = False
        #self.f = open('D:/cap/result/result.txt', 'w', encoding='utf-8')
        
        while not self.__stop:
            if self.client_sock:
                # Check if the client is still connected and if data is available:
                try:
                    rdy_read, rdy_write, sock_err = select.select([self.client_sock,], [self.client_sock,], [], 5)
                except select.error as err:
                    print('[Thr {}] Select() failed on socket with {}'.format(self.number,self.client_addr))
                    self.stop()
                    return

                if len(rdy_read) > 0:
                    read_data = self.client_sock.recv(1023)
                    #print(read_data.rstrip().decode('utf-8'))
                    if "stop" in read_data.rstrip().decode('utf-8', errors="replace") :
                        self.stop()

                    else :
                        result.append(read_data[2:].rstrip().decode('utf-8', errors="replace"))
                    
                    # Check if socket has been closed
                    if len(read_data) == 0:
                        print('[Thr {}] {} closed the socket.'.format(self.number, self.client_addr))
                        self.stop()
                    #else:
                        # Strip newlines just for output clarity
                        #print("print")
                        #print(read_data.rstrip().decode('utf-8', errors="replace"))
                
                
            else:
                print("[Thr {}] No client is connected, SocketServer can't receive data".format(self.number))
                self.stop()
        
        self.close()
        

    def stop(self):
        
        self.__stop = True
        global check
        check = False

    def close(self):
        """ Close connection with the client socket. """
        
        if self.client_sock:
            print('[Thr {}] Closing connection with {}'.format(self.number, self.client_addr))
            self.client_sock.close()
def test():
    df = pd.read_csv("./result/kko_regex.csv")
    df.Date = pd.to_datetime(df.Date)

    df["year"] = df['Date'].dt.strftime('%Y')
    df["month"] = df['Date'].dt.strftime('%m')
    df["day"] = df['Date'].dt.strftime('%d')
    df["weekday"] = df['Date'].dt.strftime('%A')

    df["24time"] = df["timetype"] + " " + df["time"]

    df.time = pd.to_datetime(df.time)


    temp = []
    transform_time = []


    for i in range(len(df)) :
        time = df["24time"][i]
        #print(time)
        temp.append(dt.datetime.strptime(time,"%p %I:%M:%S")) 
        transform_time.append(temp[i].time())
    
    df["24time"] = transform_time


    df["hh"] = df["24time"].apply(lambda x: x.strftime("%H") if pd.notnull(x) else '')
    df["mm"] = df["24time"].apply(lambda x: x.strftime("%M") if pd.notnull(x) else '')



    df.head()
    print(username)

    plt.rc('font', family='NanumGothic')


    plt.figure(2)
    sns.countplot(x="weekday", data=df)
    plt.title("요일 별 대화 수")
    plt.legend()
    # plt.savefig('./static/days.png')
    plt.savefig('./static/'+name2+'.png')
    
    plt.figure(3)
    sns.countplot(x="Speaker", data=df)
    plt.title("사용자 별 대화 수")
    plt.legend()
    # plt.savefig('./static/users.png')
    plt.savefig('./static/'+name3+'.png')
    
    plt.figure(4)
    sns.countplot(x="emotions", data=df)
    plt.title("감정의 분포")
    plt.legend()
    # plt.savefig('./static/emotion.png')
    plt.savefig('./static/'+name4+'.png')
    
    # 추가하고 싶은 부분    
    # plt.figure(4)
    # for i in username:
    #     g = sns.kdeplot(df["hh"][(df['Speaker'] == i) & (df["hh"].notnull())], bw=1.5)
    # g.set_xlabel("viewCount")
    # g.set_ylabel("Frequency")
    # plt.title("Chat Rate by Hour")
    # plt.legend()
    # plt.show()
    # plt.savefig('./static/'+name5+'.png')
       
    file_path = './static'
    file_list = os.listdir(file_path)    

    for file_name in file_list :
        old_name = file_path + '/' + file_name
        new_name = file_path + '/' + random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + random.choice(string.ascii_letters) + '.png'
        os.rename(old_name, new_name)

# def signal_handler(signal, frame):
#     file_path = './static'
#     file_list = os.listdir(file_path)
#     name = ["day.png", "wordcloud.png", "talk.png", "emotion.png"]
            
#     i = 0
#     for file_name in file_list :
#         old_name = file_path + '/' + file_name
#         new_name = file_path + '/' + name[i]
#         os.rename(old_name, new_name)
#         i = i+1

def main():
    # Start socket server, stop it after a given duration
    
    files = glob.glob('./static/*')
    for f in files:
        os.remove(f)

    
    server = SocketServer()
    server.start()

    #time.sleep(duration)
    while True :
        if check == False :
            server.stop()
            apply_kko_regex(msg_list)
            
            raw_data = pd.read_csv("./result/kko_regex.csv")
            raw_data = raw_data.dropna()
    
            raw_data["token"] = raw_data["contents"].apply(lambda x: get_noun(x))
            print(raw_data)
            raw_data.to_csv("./result/noun_token.csv", index=False)

            raw_data["token"] = raw_data["contents"].apply(lambda x: get_all_token(x))
            raw_data.to_csv("./result/all_token.csv", index=False)
            kkma_result = pd.read_csv("./result/noun_token.csv")
            draw_wordcloud(kkma_result)
            
            
            test()
            http_server = flaskServer.WSGIServer(('', 9999), flaskServer.app)
            http_server.serve_forever()
    

            break    
    server.join()
    print('End.')
    
  
if __name__ == "__main__":
    main()
    # try:
    #     main()
    # except KeyboardInterrupt :
    #     file_path = './static'
    #     file_list = os.listdir(file_path)
    #     name = ["day.png", "wordcloud.png", "talk.png", "emotion.png"]
            
    #     i = 0
    #     for file_name in file_list :
    #         old_name = file_path + '/' + file_name
    #         new_name = file_path + '/' + name[i]
    #         os.rename(old_name, new_name)
    #         i = i+1
            
    #     try:
    #         sys.exit(0)
    #     except SystemExit:
    #         os._exit(0)



