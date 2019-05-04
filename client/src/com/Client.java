package com;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

//import socket_chat_server.Server.ClientThread;
//import socket_chat_server.Server.SelecTry;

public class Client{

    private JPanel sendPanel;
    private JButton btn_send_file;
    private JFrame frame;
    private JList userList;
    private JTextArea textArea;
    private JTextField textField;
    private JTextField txt_port;
    private JTextField txt_hostIp;
    private JTextField txt_name;
    private JButton btn_start;
    private JButton btn_stop;
    private JButton btn_send;
    private JPanel northPanel;
    private JPanel southPanel;
    private JScrollPane rightScroll;
    private JScrollPane leftScroll;
    private JSplitPane centerSplit;

    private JPanel logPanle;
    private JFrame loginframe;
    private JLabel label_username;
    private JLabel label_password;
    private JTextField txt_login_name;
    private JTextField txt_password;
    private JTextField txt_login_ip;
    private JTextField txt_login_port;
    private JTextField txt_login_forget;
    private JButton btn_submit;
    private JButton btn_zhuce;
    private JButton btn_forget_pass;

    private DefaultListModel listModel;
    private boolean isConnected = false;

    private int sendfor_who;
    private int server_port=0;

    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private Socket socketfor_p2p;
    private boolean isConnected_p2p = false;
    private ArrayList<ClientThread> clients;//�ͻ��߳�����
    private PrintWriter P2P_printWriter;//��Ե����������
    private BufferedReader P2P_bufferReader;//��Ե�����������
    private MessageThread_P2P messageThread_for_p2p;// �������p2p��Ϣ���߳�
    private Map<String, Boolean> P2P_connected_user = new HashMap<String, Boolean>();

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private MessageThread messageThread;// ���������Ϣ���߳�
    private Map<String, User> onLineUsers = new HashMap<String, User>();// ���������û�
    private String myIP = "";//ÿһ���ͻ��˶���Ψһ��IP��ַ

    // ������,�������
    public static void main(String[] args) throws BindException {
        new Client();

    }

    class SelecTry implements ListSelectionListener
    {
        int change=0,who;
        public void valueChanged(ListSelectionEvent e){
            //System.out.println("you selected:"+listModel.getElementAt(userList.getSelectedIndex()));
            sendfor_who=userList.getSelectedIndex();
            isConnected_p2p = false;
        }

    }

    /**
     * ���ӷ�����
     *
     * @param port
     * @param hostIp
     * @param name
     */
    public boolean connectServer_p2p(int port, String hostIp, String name) {
        // ���ӷ�����
        try {
            socketfor_p2p = new Socket(hostIp, port);// ���ݶ˿ںźͷ�����ip��������
            P2P_printWriter = new PrintWriter(socketfor_p2p.getOutputStream());
            P2P_bufferReader = new BufferedReader(new InputStreamReader(socketfor_p2p
                    .getInputStream()));

            messageThread_for_p2p = new MessageThread_P2P(P2P_bufferReader);
            messageThread_for_p2p.start();
            P2P_connected_user.put(name,true);
            isConnected_p2p = true;// �Ѿ���������
            return true;
        } catch (Exception e) {
            textArea.append("��˿ں�Ϊ��" + port + "    IP��ַΪ��" + hostIp
                    + "   �ķ�������ʧ��!" + "\r\n");
            isConnected_p2p = false;// δ������
            return false;
        }
    }

    /**
     * �رշ���
     */
    @SuppressWarnings("deprecation")
    public void closeServer() {
        try {
            if (serverThread != null)
                serverThread.stop();// ֹͣ�������߳�

            for (int i = clients.size() - 1; i >= 0; i--) {
                // �����������û����͹ر�����
                clients.get(i).getWriter().println("CLOSE#"+frame.getTitle());
                clients.get(i).getWriter().flush();
                // �ͷ���Դ
                clients.get(i).stop();// ֹͣ����Ϊ�ͻ��˷�����߳�
                clients.get(i).reader_ptp.close();
                clients.get(i).writer_ptp.close();
                clients.get(i).socket.close();
                clients.remove(i);
            }
            if (serverSocket != null) {
                serverSocket.close();// �رշ�����������
            }
            listModel.removeAllElements();// ����û��б�
//            isStart = false;
        } catch (IOException e) {
            e.printStackTrace();
//            isStart = true;
        }
    }

    // ���Ͻ�����Ϣ���߳�
    class MessageThread_P2P extends Thread {
        private BufferedReader reader_ptp;

        // ������Ϣ�̵߳Ĺ��췽��
        public MessageThread_P2P(BufferedReader reader) {
            this.reader_ptp = reader;

        }

        // �����Ĺر�����
        public synchronized void closeCon() throws Exception {
            System.out.println("close :*************");
            // �����Ĺر������ͷ���Դ
            if (reader_ptp != null) {
                reader_ptp.close();
            }
            if (P2P_printWriter != null) {
                P2P_printWriter.close();
            }
            if (socketfor_p2p != null) {
                socketfor_p2p.close();
            }
            isConnected_p2p = false;// �޸�״̬Ϊ�Ͽ�

        }

        public void run() {
            String message = "";
            while (true) {
                try {
                    message = reader_ptp.readLine();
                    StringTokenizer stringTokenizer = new StringTokenizer(
                            message, "/#");
                    String command = stringTokenizer.nextToken();// ����
                    if (command.equals("CLOSE"))// �������ѹر�����
                    {
                        String user = stringTokenizer.nextToken();
                        textArea.append("�û� "+user+"  �����ߣ�p2p�����ѹر�!\r\n");
                        closeCon();// �����Ĺر�����
                        JOptionPane.showMessageDialog(frame, "�û� "+user+"  �����ߣ�p2p�����ѹر�!", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;// �����߳�
                    } else if (command.equals("FILE")) {
                        int portNumber = Integer.parseInt(stringTokenizer.nextToken());
                        String fileName = stringTokenizer.nextToken();
                        long fileSize = Long.parseLong(stringTokenizer.nextToken());
                        String ip = stringTokenizer.nextToken();
                        String Nickname = stringTokenizer.nextToken();
                        ReceiveFileThread receiveFile = new ReceiveFileThread(textArea,frame,ip, portNumber, fileName, fileSize, Nickname);
                        receiveFile.start();
                        textArea.append("�� "+Nickname+" �����ļ�:"+fileName+",��СΪ:"+fileSize
                                +"ip:"+ip+"port:"+portNumber+"\r\n");
                    } else {// ��ͨ��Ϣ
                        textArea.append(""+message + "\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * ����������
     *
     * @param port
     * @throws java.net.BindException
     */
    public void serverStart(int port) throws java.net.BindException {
        try {
            clients = new ArrayList<ClientThread>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread(serverSocket);
            serverThread.start();
            server_port = serverSocket.getLocalPort();
            InetAddress addr = InetAddress.getLocalHost();
            myIP = addr.getHostAddress();//��ñ���IP
//            myIP = serverSocket.getInetAddress().getHostAddress();
            System.out.println("mmyIP=="+myIP+"\r\n");
        } catch (BindException e) {
            throw new BindException("�˿ں��ѱ�ռ�ã��뻻һ����");
        } catch (Exception e1) {
            e1.printStackTrace();
            throw new BindException("�����������쳣��");
        }
    }

    /**
     * Ϊ��һ���������ӵĿͻ����ṩ������߳�
     */
    class ClientThread extends Thread {
        private Socket socket;
        private BufferedReader reader_ptp;
        private PrintWriter writer_ptp;
        private User user;

        public BufferedReader getReader() {
            return reader_ptp;
        }

        public PrintWriter getWriter() {
            return writer_ptp;
        }

        public User getUser() {
            return user;
        }

        // �ͻ����̵߳Ĺ��췽��
        public ClientThread(Socket socket) {
            try {
                this.socket = socket;
                reader_ptp = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
                writer_ptp = new PrintWriter(socket.getOutputStream());

                // ���տͻ��˵Ļ����û���Ϣ
                String inf = reader_ptp.readLine();
                StringTokenizer st = new StringTokenizer(inf, "#");
                user = new User(st.nextToken(), socket.getLocalAddress().toString());
                // �������ӳɹ���Ϣ
                writer_ptp.println(frame.getTitle()+"  ����˵��  "+user.getName()+"/"+user.getIp()+"��ã�"+"����"+frame.getTitle()+"�������ӳɹ���");
                writer_ptp.flush();
//                // ������ǰ�����û���Ϣ
//                if (clients.size() > 0) {
//                    String temp = "";
//                    for (int i = clients.size() - 1; i >= 0; i--) {
//                        temp += (clients.get(i).getUser().getName() + "/" + clients
//                                .get(i).getUser().getIp())
//                                + "#";
//                    }
//                    writer.println("USERLIST#" + clients.size() + "#" + temp);
//                    writer.flush();
//                }
//                // �����������û����͸��û���������
//                for (int i = clients.size() - 1; i >= 0; i--) {
//                    clients.get(i).getWriter().println(
//                            "ADD#" + user.getName() + user.getIp());
//                    clients.get(i).getWriter().flush();
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        public void run() {// ���Ͻ��տͻ��˵���Ϣ�����д���
            String message = null;
            while (true) {
                try {
                    message = reader_ptp.readLine();// ���տͻ�����Ϣ
                    StringTokenizer stringTokenizer = new StringTokenizer(message,"/#");
                    String command = stringTokenizer.nextToken();
                    if (command.equals("CLOSE"))// ��������
                    {
                        textArea.append("��"+this.getUser().getName()
                                + this.getUser().getIp() + "�������ӳɹ�!\r\n");
                        // �Ͽ������ͷ���Դ
                        this.getUser().setState(0);
                        reader.close();
                        writer.close();
                        socket.close();

                    } else if (command.equals("FILE")) {
                        int portNumber = Integer.parseInt(stringTokenizer.nextToken());
                        String fileName = stringTokenizer.nextToken();
                        long fileSize = Long.parseLong(stringTokenizer.nextToken());
                        String ip = stringTokenizer.nextToken();
                        String Nickname = stringTokenizer.nextToken();
                        ReceiveFileThread receiveFile = new ReceiveFileThread(textArea,frame,ip, portNumber, fileName, fileSize, Nickname);
                        receiveFile.start();
                        textArea.append("�� "+Nickname+" �����ļ� :"+fileName+",��СΪ:  "+fileSize
                                +"   ip: "+ip+"    port:"+portNumber+"\r\n");
                    }else {
                        textArea.append(user.getName()+"  ����˵�� "+message+"\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * �����߳�
     */
    class ServerThread extends Thread {
        private ServerSocket serverSocket;

        // �������̵߳Ĺ��췽��
        public ServerThread(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {
            while (true) {// ��ͣ�ĵȴ��ͻ��˵�����
                try {
                    Socket socket = serverSocket.accept();
                    ClientThread client = new ClientThread(socket);
                    client.start();// �����Դ˿ͻ��˷�����߳�
                    clients.add(client);
                    textArea.append("�����û�p2p����\r\n");
//                    user_name_update();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void sendFile() {
        //�ļ�ѡ��Ի�����������ѡ�����ļ��Ժ��ÿһ��client�����ļ�
        JFileChooser sourceFileChooser = new JFileChooser(".");
        sourceFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int status = sourceFileChooser.showOpenDialog(frame);
        File sourceFile = new File(sourceFileChooser.getSelectedFile().getPath());
        //������text area��ʾ
        textArea.append("�����ļ���" + sourceFile.getName() + "\r\n");

        if(sendfor_who==0){
            textArea.append("�Է����������ļ�!");
        }else{
            StringTokenizer st = new StringTokenizer(listModel.getElementAt(sendfor_who)+"", "---()");
            String user_name = st.nextToken();
            String user_state = st.nextToken();
            if (user_state.equals("����")) {
                for (int i = clients.size()-1; i >= 0; i--) {
                    if (clients.get(i).getUser().getName().equals(user_name)) {
                        SendFileThread sendFile = new SendFileThread(frame, clients.get(i).socket, frame.getTitle(), sourceFileChooser, status);
                        sendFile.start();
                        //client����ʾ
                        textArea.append("��  "+user_name+"  ����һ���ļ���" + sourceFile.getName() + "\r\n");
                        return;
                    }
                }
                SendFileThread sendFile = new SendFileThread(frame, socketfor_p2p, frame.getTitle(), sourceFileChooser, status);
                sendFile.start();
                //client����ʾ
                textArea.append("��  "+user_name+"  ����һ���ļ���" + sourceFile.getName() + "\r\n");
            }else{
                JOptionPane.showMessageDialog(frame, "�û������ߣ����ܷ����ļ���");
            }
        }

    }



    // ִ�з���
    public void send() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(frame, "��û�����ӷ��������޷�������Ϣ��", "����",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String message = textField.getText().trim();
        if (message == null || message.equals("")) {
            JOptionPane.showMessageDialog(frame, "��Ϣ����Ϊ�գ�", "����",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        //sendMessage(frame.getTitle() + "#" + "ALL" + "#" + message);
        if(sendfor_who==0){
            sendMessage(frame.getTitle() + "#" + "ALL" + "#" + message);
            textField.setText(null);
        }else{
            StringTokenizer st = new StringTokenizer(listModel.getElementAt(sendfor_who)+"", "---()");
            String user_name = st.nextToken();
            String user_state = st.nextToken();
            System.out.print("user_state:" + user_state);
            if (user_state.equals("����")) {
                for (int i = clients.size()-1; i >= 0; i--) {
                    if (clients.get(i).getUser().getName().equals(user_name)) {
                        clients.get(i).writer_ptp.println("�� "+user_name+"  ˵��  "+message+"\r\n");
                        clients.get(i).writer_ptp.flush();
                        textArea.append("��  "+user_name+"  ˵�� "+message+"\r\n");
                        textField.setText(null);
                        return;
                    }
                }
                if (!isConnected_p2p) {
                    JOptionPane.showMessageDialog(frame, "��Ե㼴�����ӣ�");
                    sendMessage("P2P#"+user_name);
                }else{
                    P2P_printWriter.println(message);
                    P2P_printWriter.flush();
                    textArea.append("��  "+user_name+"  ˵�� "+message+"\r\n");
                    textField.setText(null);
                }

            }else{
                JOptionPane.showMessageDialog(frame, "�û������ߣ��Ѵ�Ϊ���ԣ�");
                sendMessage("LIXIAN#"+frame.getTitle() + "#" + user_name + "#" + message);
                textArea.append("��  "+user_name+"  ���ԣ� "+message+"\r\n");
                textField.setText(null);
            }
        }
    }

    public void Login(){
        Font font = new Font("����", 1, 16);

        logPanle = new JPanel();
        //logPanle.setLayout(new GridLayout(2, 2));
        logPanle.setBounds(2, 45, 250, 225);
        logPanle.setBackground(Color.lightGray);
        logPanle.setLayout(new GridLayout(5, 2, 20, 20));

        label_username = new JLabel("�û���:");
        label_username.setFont(font);
        label_username.setHorizontalAlignment(SwingConstants.CENTER);
        logPanle.add(label_username);

        txt_login_name = new JTextField("name");
        txt_login_name.setFont(font);
        logPanle.add(txt_login_name);

        label_password = new JLabel("�� ��:");
        label_password.setFont(font);
        label_password.setHorizontalAlignment(SwingConstants.CENTER);
        logPanle.add(label_password);

        txt_password = new JTextField("");
        txt_password.setFont(font);
        logPanle.add(txt_password);

        txt_login_ip = new JTextField("127.0.0.1");
        txt_login_ip.setFont(font);
        logPanle.add(txt_login_ip);

        txt_login_port = new JTextField("6666");
        txt_login_port.setFont(font);
        logPanle.add(txt_login_port);

        logPanle.add(txt_login_ip);
        logPanle.add(txt_login_port);

        btn_submit = new JButton("��½");
        btn_submit.setFont(font);
        btn_submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int port;
                String message_name = txt_login_name.getText().trim();
                if (message_name == null || message_name.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "�û�������Ϊ�գ�", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_pw = txt_password.getText().trim();
                if (message_pw == null || message_pw.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "���벻��Ϊ�գ�", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (isConnected) {
                    String message1 = txt_login_name.getText().trim();
                    if (message1 == null || message1.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "�û�������Ϊ�գ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message2 = txt_password.getText().trim();
                    if (message2 == null || message2.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "���벻��Ϊ�գ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    sendMessage("USERLOGIN#"+message1+"#"+message2+"#"+server_port+"#"+myIP);
                    return;
                }else{
                    try {
                        try {
                            port = Integer.parseInt(txt_login_port.getText().trim());
                        } catch (NumberFormatException e2) {
                            throw new Exception("�˿ںŲ�����Ҫ��!�˿�Ϊ����!");
                        }
                        String hostIp = txt_login_ip.getText().trim();
                        String name = txt_login_name.getText().trim();
                        if (name.equals("") || hostIp.equals("")) {
                            throw new Exception("������������IP����Ϊ��!");
                        }
                        boolean flag = connectServer(port, hostIp, name);
                        if (flag == false) {
                            throw new Exception("�����������ʧ��!");
                        }
                        frame.setTitle(name);
                        listModel.addElement("ȫ���û�");
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(loginframe, exc.getMessage(),
                                "����", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                sendMessage("USERLOGIN#"+message_name+"#"+message_pw+"#"+server_port+"#"+myIP);
            }
        });
        logPanle.add(btn_submit);

        btn_zhuce = new JButton("ע��");
        btn_zhuce.setFont(font);
        btn_zhuce.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int port;
                String message_name = txt_login_name.getText().trim();
                if (message_name == null || message_name.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "�û�������Ϊ�գ�", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_pw = txt_password.getText().trim();
                if (message_pw == null || message_pw.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "���벻��Ϊ�գ�", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_yx = txt_login_forget.getText().trim();
                if (message_yx == null || message_yx.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "ע�����䲻��Ϊ�գ�", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (isConnected) {
                    String message1 = txt_login_name.getText().trim();
                    if (message1 == null || message1.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "�û�������Ϊ�գ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message2 = txt_password.getText().trim();
                    if (message2 == null || message2.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "���벻��Ϊ�գ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message3 = txt_login_forget.getText().trim();
                    if (message3 == null || message3.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "ע�����䲻��Ϊ�գ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    sendMessage("USERZHUCE#"+message1+"#"+message2+"#"+message3);
                    return;
                }else{
                    try {
                        try {
                            port = Integer.parseInt(txt_login_port.getText().trim());
                        } catch (NumberFormatException e2) {
                            throw new Exception("�˿ںŲ�����Ҫ��!�˿�Ϊ����!");
                        }
                        String hostIp = txt_login_ip.getText().trim();
                        String name = txt_login_name.getText().trim();
                        if (name.equals("") || hostIp.equals("")) {
                            throw new Exception("������������IP����Ϊ��!");
                        }
                        boolean flag = connectServer(port, hostIp, name);
                        if (flag == false) {
                            throw new Exception("�����������ʧ��!");
                        }
                        frame.setTitle(name);
                        listModel.addElement("ȫ���û�");
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(frame, exc.getMessage(),
                                "����", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }


                sendMessage("USERZHUCE#"+message_name+"#"+message_pw+"#"+message_yx);
            }
        });
        logPanle.add(btn_zhuce);



        txt_login_forget = new JTextField("");
        txt_login_forget.setFont(font);
        logPanle.add(txt_login_forget);

        btn_forget_pass = new JButton("�����һ�");
        btn_forget_pass.setFont(font);
        btn_forget_pass.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int port;

                String message_name = txt_login_name.getText().trim();
                if (message_name == null || message_name.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "�û�������Ϊ�գ�", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_yx = txt_login_forget.getText().trim();
                if (message_yx == null || message_yx.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "ע�����䲻��Ϊ�գ�", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String message_pw = txt_password.getText().trim();
                if (message_pw == null || message_pw.equals("")) {
                    JOptionPane.showMessageDialog(logPanle, "�޸����벻��Ϊ�գ�", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (isConnected) {
                    String message1 = txt_login_name.getText().trim();
                    if (message1 == null || message1.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "�û�������Ϊ�գ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message2 = txt_login_forget.getText().trim();
                    if (message2 == null || message2.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "ע�����䲻��Ϊ�գ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String message3 = txt_password.getText().trim();
                    if (message3 == null || message3.equals("")) {
                        JOptionPane.showMessageDialog(logPanle, "�޸����벻��Ϊ�գ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    sendMessage("USERFORGET#"+message1+"#"+message2+"#"+message3);
                    return;
                }else{
                    try {
                        try {
                            port = Integer.parseInt(txt_login_port.getText().trim());
                        } catch (NumberFormatException e2) {
                            throw new Exception("�˿ںŲ�����Ҫ��!�˿�Ϊ����!");
                        }
                        String hostIp = txt_login_ip.getText().trim();
                        String name = txt_login_name.getText().trim();
                        if (name.equals("") || hostIp.equals("")) {
                            throw new Exception("������������IP����Ϊ��!");
                        }
                        boolean flag = connectServer(port, hostIp, name);
                        
                        if (flag == false) {
                            throw new Exception("�����������ʧ��!");
                        }
                        frame.setTitle(name);
                        listModel.addElement("ȫ���û�");
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(frame, exc.getMessage(),
                                "����", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                sendMessage("USERFORGET#"+message_name+"#"+message_yx+"#"+message_pw);
            }
        });
        logPanle.add(btn_forget_pass);


        logPanle.setBorder(new TitledBorder("��½"));
        loginframe = new JFrame("��½����");
        loginframe.add(logPanle,"Center");
        loginframe.setSize(300, 300);
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        loginframe.setLocation((screen_width - loginframe.getWidth()) / 2,
                (screen_height - loginframe.getHeight()) / 2);
        loginframe.setVisible(true);

        // �رմ���ʱ�¼�
        loginframe.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isConnected) {
                    closeConnection();// �ر�����
                    closeServer();//�رշ������
                }
                System.exit(0);// �˳�����
            }
        });
    }

    // ���췽��
    public Client() throws BindException {

        serverStart(0);

        SelecTry selectIndex=new SelecTry();

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setForeground(Color.blue);
        textField = new JTextField();
        txt_port = new JTextField("6666");
        txt_hostIp = new JTextField("127.0.0.1");
        txt_name = new JTextField("����");
        btn_start = new JButton("����");
        btn_stop = new JButton("�Ͽ�");
        btn_send = new JButton("����");
        btn_send_file = new JButton("�ļ�");
        listModel = new DefaultListModel();
        userList = new JList(listModel);
        //listModel.addElement("ȫ���û�");
        userList.addListSelectionListener(selectIndex);
        northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1, 7));
        northPanel.add(new JLabel("     �˿�"));
        northPanel.add(txt_port);
        northPanel.add(new JLabel("    ������IP"));
        northPanel.add(txt_hostIp);
        northPanel.add(new JLabel("     ����"));
        northPanel.add(txt_name);
        northPanel.add(btn_start);
        northPanel.add(btn_stop);
        northPanel.setBorder(new TitledBorder("������Ϣ"));

        rightScroll = new JScrollPane(textArea);
        rightScroll.setBorder(new TitledBorder("��Ϣ��ʾ��"));
        leftScroll = new JScrollPane(userList);
        leftScroll.setBorder(new TitledBorder("�û��б�"));
        southPanel = new JPanel(new BorderLayout());
        sendPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new TitledBorder("д��Ϣ"));
        southPanel.add(textField, "Center");
        sendPanel.add(btn_send, BorderLayout.NORTH);
        sendPanel.add(btn_send_file, BorderLayout.SOUTH);
        southPanel.add(sendPanel, "East");


        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll,
                rightScroll);
        centerSplit.setDividerLocation(150);
        
        System.out.print("�ͻ���");
        frame = new JFrame("�ͻ���");
        // ����JFrame��ͼ�꣺
        //frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class.getResource("qq.png")));
        frame.setLayout(new BorderLayout());
        frame.add(northPanel, "North");
        frame.add(centerSplit, "Center");
        frame.add(southPanel, "South");
        frame.setSize(600, 400);
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((screen_width - frame.getWidth()) / 2,
                (screen_height - frame.getHeight()) / 2);
        frame.setVisible(false);

        // д��Ϣ���ı����а��س���ʱ�¼�
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            	System.out.print("send");
                send();
            }
        });
        
        // �������Ͱ�ťʱ�¼�
        btn_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	System.out.print("send1");
                send();
            }
        });

        //�����ļ���ťʱ�¼�
        btn_send_file.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            	System.out.print("sendfile");
                sendFile();
            }
        });

        // �������Ӱ�ťʱ�¼�
//        btn_start.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                int port;
//                if (isConnected) {
//                    JOptionPane.showMessageDialog(frame, "�Ѵ���������״̬����Ҫ�ظ�����!",
//                            "����", JOptionPane.ERROR_MESSAGE);
//                    return;
//                }
//                try {
//                    try {
//                        port = Integer.parseInt(txt_port.getText().trim());
//                    } catch (NumberFormatException e2) {
//                        throw new Exception("�˿ںŲ�����Ҫ��!�˿�Ϊ����!");
//                    }
//                    String hostIp = txt_hostIp.getText().trim();
//                    String name = txt_name.getText().trim();
//                    if (name.equals("") || hostIp.equals("")) {
//                        throw new Exception("������������IP����Ϊ��!");
//                    }
//                    boolean flag = connectServer(port, hostIp, name);
//                    if (flag == false) {
//                        throw new Exception("�����������ʧ��!");
//                    }
//                    listModel.addElement("ȫ���û�");
//                    frame.setTitle(name);
//                    JOptionPane.showMessageDialog(frame, "�ɹ�����!");
//                } catch (Exception exc) {
//                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
//                            "����", JOptionPane.ERROR_MESSAGE);
//                }
//            }
//        });

        // �����Ͽ���ťʱ�¼�
        btn_stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	System.out.print("stop");
                if (!isConnected) {
                    JOptionPane.showMessageDialog(frame, "�Ѵ��ڶϿ�״̬����Ҫ�ظ��Ͽ�!",
                            "����", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    boolean flag = closeConnection();// �Ͽ�����
                    closeServer();
                    if (flag == false) {
                        throw new Exception("�Ͽ����ӷ����쳣��");
                    }
                    JOptionPane.showMessageDialog(frame, "�ɹ��Ͽ�!");
                    frame.setVisible(false);
                    textArea.setText("");
                    loginframe.setVisible(true);
                    listModel.removeAllElements();
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "����", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // �رմ���ʱ�¼�
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
            	System.out.print("close");
                if (isConnected) {
                    closeConnection();// �ر�����
                    closeServer();//�رշ������
                }
                System.exit(0);// �˳�����
            }
        });
        System.out.print("login");
        Login();
    }

    /**
     * ���ӷ�����
     *
     * @param port
     * @param hostIp
     * @param name
     */
    public boolean connectServer(int port, String hostIp, String name) {
        // ���ӷ�����
        try {
            socket = new Socket(hostIp, port);// ���ݶ˿ںźͷ�����ip��������
            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
            // ���Ϳͻ����û�������Ϣ(�û�����ip��ַ)
            sendMessage(name + "#" + socket.getLocalAddress().toString());
            // ����������Ϣ���߳�
            messageThread = new MessageThread(reader, textArea);
            messageThread.start();
            isConnected = true;// �Ѿ���������
            return true;
        } catch (Exception e) {
            textArea.append("��˿ں�Ϊ��" + port + "    IP��ַΪ��" + hostIp
                    + "   �ķ���������ʧ��!" + "\r\n");
            isConnected = false;// δ������
            return false;
        }
    }

    /**
     * ������Ϣ
     *
     * #param message
     */
    public void sendMessage(String message) {
        writer.println(message);
        writer.flush();
    }

    /**
     * �ͻ��������ر�����
     */
    @SuppressWarnings("deprecation")
    public synchronized boolean closeConnection() {
        try {
            sendMessage("CLOSE");// ���ͶϿ����������������
            messageThread.stop();// ֹͣ������Ϣ�߳�
            // �ͷ���Դ
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false;
            listModel.removeAllElements();
            return true;
        } catch (IOException e1) {
            e1.printStackTrace();
            isConnected = true;
            return false;
        }
    }

    // ���Ͻ�����Ϣ���߳�
    class MessageThread extends Thread {
        private BufferedReader reader;
        private JTextArea textArea;

        // ������Ϣ�̵߳Ĺ��췽��
        public MessageThread(BufferedReader reader, JTextArea textArea) {
            this.reader = reader;
            this.textArea = textArea;
        }

        // �����Ĺر�����
        public synchronized void closeCon() throws Exception {
            // ����û��б�
            listModel.removeAllElements();
            // �����Ĺر������ͷ���Դ
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false;// �޸�״̬Ϊ�Ͽ�

        }

        public void run() {
            String message = "";
            while (true) {
                try {
                    message = reader.readLine();
                    StringTokenizer stringTokenizer = new StringTokenizer(
                            message, "/#");
                    String command = stringTokenizer.nextToken();// ����
                    if (command.equals("CLOSE"))// �������ѹر�����
                    {
                        textArea.append("�������ѹر�!\r\n");
                        closeCon();// �����Ĺر�����
                        JOptionPane.showMessageDialog(frame, "�������ѹرգ�", "����",
                                JOptionPane.ERROR_MESSAGE);
                        frame.setVisible(false);
                        textArea.setText("");
                        loginframe.setVisible(true);
                        return;// �����߳�
                    } else if (command.equals("ADD")) {// ���û����߸��������б�
                        String username = "";
                        String userIp = "";
                        if ((username = stringTokenizer.nextToken()) != null
                                && (userIp = stringTokenizer.nextToken()) != null) {
                            User user = new User(username, userIp);
                            onLineUsers.put(username, user);
                            listModel.addElement(username);
                        }
                    } else if (command.equals("DELETE")) {// ���û����߸��������б�
                        String username = stringTokenizer.nextToken();
                        User user = (User) onLineUsers.get(username);
                        onLineUsers.remove(user);
                        listModel.removeElement(username);
                    } else if (command.equals("USERLIST")) {// ���������û��б�
                        listModel.removeAllElements();
                        listModel.addElement("ȫ������");
                        StringTokenizer strToken ;
                        String user ;// ����
                        int size = Integer
                                .parseInt(stringTokenizer.nextToken());
                        String username = null;
                        String userIp = null;
                        for (int i = 0; i < size-1; i++) {
                            username = stringTokenizer.nextToken();
                            strToken = new StringTokenizer(username, "---()");
                            if (strToken.nextToken().equals(frame.getTitle())) {
                                continue;
                            }else{
                                listModel.addElement(username);
                            }
                            //userIp = stringTokenizer.nextToken();
                            //User user = new User(username, userIp);
                            //onLineUsers.put(username, user);
                        }
                    } else if (command.equals("MAX")) {// �����Ѵ�����
                        textArea.append(stringTokenizer.nextToken()
                                + stringTokenizer.nextToken() + "\r\n");
                        closeCon();// �����Ĺر�����
                        JOptionPane.showMessageDialog(frame, "������������������", "����",
                                JOptionPane.ERROR_MESSAGE);
                        return;// �����߳�
                    } else if(command.equals("FILE")){
                        int portNumber = Integer.parseInt(stringTokenizer.nextToken());
                        String fileName = stringTokenizer.nextToken();
                        long fileSize = Long.parseLong(stringTokenizer.nextToken());
                        String ip = stringTokenizer.nextToken();
                        String Nickname = stringTokenizer.nextToken();
                        ReceiveFileThread receiveFile = new ReceiveFileThread(textArea,frame,ip, portNumber, fileName, fileSize, Nickname);
                        receiveFile.start();
                        textArea.append("��"+Nickname+"�����ļ�:"+fileName+",��СΪ:"+fileSize
                                +"ip:"+ip+"port:"+portNumber+"\r\n");
                    }else if(command.equals("USERLOGIN")){
                        String st = stringTokenizer.nextToken();
                        if(st.equals("OK")){
                            JOptionPane.showMessageDialog(loginframe, "��½�ɹ�!" );
                            loginframe.setVisible(false);
                            txt_name.setText(txt_login_name.getText());
                            frame.setVisible(true);
                            int count = stringTokenizer.countTokens();
                            while(true){
                                if(count==0){
                                    break;
                                }
                                textArea.append(stringTokenizer.nextToken()+"  �������� ��");
                                textArea.append("ʱ�䣺 "+stringTokenizer.nextToken()+"\r\n   ");
                                textArea.append("�������ݣ� "+stringTokenizer.nextToken()+"\r\n");
                                count-=3;
                            }

                        }else if(st.equals("ALREADY")){
                            JOptionPane.showMessageDialog(loginframe, "�˺��ѵ�½!" );
                        }else{
                            JOptionPane.showMessageDialog(loginframe, "��½ʧ��!" );
                        }
                    }else if(command.equals("USERZHUCE")){
                        String st = stringTokenizer.nextToken();
                        if(st.equals("OK")){
                            JOptionPane.showMessageDialog(loginframe, "ע��ɹ�!" );

                        }else if(st.equals("exict")){
                            JOptionPane.showMessageDialog(loginframe, "�û����Ѵ���!" );
                        }else{
                            JOptionPane.showMessageDialog(loginframe, "ע��ʧ��!" );
                        }
                    }else if(command.equals("USERFORGET")){
                        String st = stringTokenizer.nextToken();
                        if(st.equals("OK")){
                            JOptionPane.showMessageDialog(loginframe, "�޸�����ɹ�!" );

                        }else if(st.equals("YOUXIANG_WRONG")){
                            JOptionPane.showMessageDialog(loginframe, "�������!" );
                        }else if(st.equals("NAME_NO_exict")){
                            JOptionPane.showMessageDialog(loginframe, "�û�������!" );
                        }else{
                            JOptionPane.showMessageDialog(loginframe, "�һ�����ʧ��!" );
                        }
                    } else if (command.equals("P2P")) {
                        String st = stringTokenizer.nextToken();
                        if(st.equals("OK")){
                            String username = stringTokenizer.nextToken();
                            int serverPort = Integer.parseInt(stringTokenizer.nextToken());
                            String ip = stringTokenizer.nextToken();
                            boolean cn = connectServer_p2p(serverPort,ip,username);
                            if (cn) {
                                JOptionPane.showMessageDialog(frame, "��"+username+"�����ӳɹ����˿ں�Ϊ��"+serverPort+"IP:"+ip );
                                P2P_printWriter.println(frame.getTitle()+"#"+myIP);
                                P2P_printWriter.flush();

                                String msg = textField.getText().trim();
                                P2P_printWriter.println(msg);
                                P2P_printWriter.flush();

                                textArea.append("��  "+username+"  ˵�� "+msg+"\r\n");

                                textField.setText(null);
                            }

                        }else{
                            String username = stringTokenizer.nextToken();
                            JOptionPane.showMessageDialog(frame, "��"+username+"������ʧ�ܣ�");
                        }
                    } else {// ��ͨ��Ϣ
                        textArea.append(message + "\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
