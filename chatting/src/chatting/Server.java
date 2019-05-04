package chatting;
import sun.misc.BASE64Encoder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class Server {

    private JFrame frame;
    private JTextArea contentArea;
    private JTextField txt_message;
    private JTextField txt_max;
    private JTextField txt_port;
    private JButton btn_start;
    private JButton btn_stop;
    private JButton btn_send;
    private JButton btn_send_file;
    private JPanel northPanel;
    private JPanel southPanel;
    private JPanel sendPanel;
    private JScrollPane rightPanel;
    private JScrollPane leftPanel;
    private JScrollPane rightPanel2;
    private JSplitPane centerSplit;
    private JSplitPane centerSplit2;
    private JList userList;
    private JList all_userList;
    private DefaultListModel listModel;
    private static DefaultListModel<String> all_listModel;

    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private ArrayList<ClientThread> clients;//�ͻ��߳�����

    private boolean isStart = false;//��־�������Ƿ�������ر�

    private int sendfor_who = 0;//�������jlist��������ĸ��û�����Ϣ

    // ������,����ִ�����
    public static void main(String[] args) {

        new Server();

        try {
            Connection con = null; //����һ��MYSQL���Ӷ���
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
            Statement stmt; //��������
            stmt = con.createStatement();
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
    }

    /**����MD5���м���
     * @param str  �����ܵ��ַ���
     * @return  ���ܺ���ַ���
     * @throws NoSuchAlgorithmException  û�����ֲ�����ϢժҪ���㷨
     * @throws UnsupportedEncodingException
     */
    public String EncoderByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //ȷ�����㷽��
        MessageDigest md5=MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //���ܺ���ַ���
        String newstr=base64en.encode(md5.digest(str.getBytes("utf-8")));
        return newstr;
    }

    /**�ж��û������Ƿ���ȷ
     * @param newpasswd  �û����������
     * @param oldpasswd  ���ݿ��д洢�����룭���û������ժҪ
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public boolean checkpassword(String newpasswd,String oldpasswd) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        if(EncoderByMd5(newpasswd).equals(oldpasswd))
            return true;
        else
            return false;
    }

    /**
     * �շ�����ͻȻ�ر�ʱ���������û�״̬��Ϊ����
     */
    public void set_user_state_off() {
        try {
            Connection con = null; //����һ��MYSQL���Ӷ���
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
            Statement stmt; //��������
            stmt = con.createStatement();

            int id = 0;
            String selectSql = "UPDATE user SET state = 0";
            stmt.executeUpdate(selectSql);
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
    }

    /**
     * �����û�״̬
     */
    public void user_name_update() {

        try {
            Connection con = null; //����һ��MYSQL���Ӷ���
            Class.forName("com.mysql.jdbc.Driver");//MYSQL����
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
            Statement stmt; //��������
            stmt = con.createStatement();

            all_listModel.removeAllElements();
            all_listModel.addElement("ȫ���û�");

            String username_db;
            int state = 0;
            //��ѯ�û���
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //ѭ����������
                username_db = selectRes.getString("username");
                state = selectRes.getInt("state");
                if (state == 1) {
                    all_listModel.addElement(username_db + "---(����)");
                }

            }

            selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //ѭ����������
                username_db = selectRes.getString("username");
                state = selectRes.getInt("state");
                if (state == 0) {
                    all_listModel.addElement(username_db + "---(����)");
                }

            }
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }

    }
//    /**
//     * ���º���״̬
//     */
//    public void user_friend_update(String name) {
//
//        try {
//            Connection con = null; //����һ��MYSQL���Ӷ���
//            Class.forName("com.mysql.jdbc.Driver");//MYSQL����
//            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
//            Statement stmt; //��������
//            stmt = con.createStatement();
//
//            all_listModel.removeAllElements();
//            all_listModel.addElement("ȫ������");
//
//            String username_db, idtype = null;
//            int state = 0;
//            int id = 0;
//            int[] d;
//            d = new int [30];
//            String selectSql = "SELECT * FROM user";
//            ResultSet selectRes = stmt.executeQuery(selectSql);
//            while (selectRes.next()) { //ѭ����������
//                username_db = selectRes.getString("username");
//                if (username_db.equals(name)) {
//                    idtype = selectRes.getString("friend");
//                }
//            }
//            StringTokenizer stringTokenizer = new StringTokenizer(
//                    idtype, " ");
//            String i;
//            int k = 0;
//            while(stringTokenizer.hasMoreTokens())
//            {
//            	i = stringTokenizer.nextToken();
//            	System.out.print("friend:" + i +"\n");
//            	d[k] = Integer.parseInt(i);
//            	System.out.print("friend1:" + d[k]+"\n");
//            	k++;
//            }
//            int flag = 0, t = 0;
//            //��ѯ�û���
//            selectRes = stmt.executeQuery(selectSql);
//            while (selectRes.next()) { //ѭ����������
//                username_db = selectRes.getString("username");
//                state = selectRes.getInt("state");
//                id = selectRes.getInt("Id");
//                for(t = 0; t < k; t++)
//                {
//                	if(d[t] == id) {
//                		flag = 0;
//                		break;
//                	}
//                	else flag = 1;
//                }
//                if(flag == 1) continue;
//                if (state == 1) {
//                    all_listModel.addElement(username_db + "---(����)");
//                }
//
//            }
//                
//            selectRes = stmt.executeQuery(selectSql);
//            while (selectRes.next()) { //ѭ����������
//                username_db = selectRes.getString("username");
//                state = selectRes.getInt("state");
//                id = selectRes.getInt("Id");
//                for(t = 0; t < k; t++)
//                {
//                	if(d[t] == id) {
//                		flag = 0;
//                		break;
//                	}
//                	else flag = 1;
//                }
//                if(flag == 1) continue;
//                if (state == 0) {
//                    all_listModel.addElement(username_db + "---(����)");
//                }
//
//            }
//        } catch (Exception e) {
//            System.out.print("MYSQL ERROR:" + e.getMessage());
//        }
//
//    }
    /**
     * ִ����Ϣ����
     */
    public void send() {
        if (!isStart) {
            JOptionPane.showMessageDialog(frame, "��������δ����,���ܷ�����Ϣ��", "����",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
//        if (clients.size() == 0) {
//            JOptionPane.showMessageDialog(frame, "û���û�����,���ܷ�����Ϣ��", "����",
//                    JOptionPane.ERROR_MESSAGE);
//            return;
//        }
        String message = txt_message.getText().trim();
        if (message == null || message.equals("")) {
            JOptionPane.showMessageDialog(frame, "��Ϣ����Ϊ�գ�", "����",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        sendServerMessage(message, sendfor_who);// Ⱥ����������Ϣ
        //contentArea.append("��  "+listModel.getElementAt(sendfor_who)+"  ˵��" + txt_message.getText() + "\r\n");
        txt_message.setText(null);
    }

    // ����ŷ�
    public Server() {
        SelecTry selectIndex = new SelecTry();
        frame = new JFrame("������");
        contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setForeground(Color.blue);
        txt_message = new JTextField();
        txt_max = new JTextField("30");
        txt_port = new JTextField("6666");
        btn_start = new JButton("����");
        btn_stop = new JButton("ֹͣ");
        btn_send = new JButton("����");
        btn_send_file = new JButton("�ļ�");
        btn_stop.setEnabled(false);
        listModel = new DefaultListModel();
        all_listModel = new DefaultListModel();
        //listModel.addElement("ȫ���û�");
        userList = new JList(all_listModel);//listModel
        userList.addListSelectionListener(selectIndex);
        user_name_update();//�����û�״̬      

//        all_userList = new JList(all_listModel);

        southPanel = new JPanel(new BorderLayout());
        sendPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new TitledBorder("д��Ϣ"));
        southPanel.add(txt_message, "Center");
        sendPanel.add(btn_send, BorderLayout.NORTH);
        sendPanel.add(btn_send_file, BorderLayout.SOUTH);

        southPanel.add(sendPanel, "East");

        leftPanel = new JScrollPane(userList);
        leftPanel.setBorder(new TitledBorder("�û��б�"));

//        rightPanel2 = new JScrollPane(all_userList);
//        rightPanel2.setBorder(new TitledBorder("ȫ���û�"));

        rightPanel = new JScrollPane(contentArea);
        rightPanel.setBorder(new TitledBorder("��Ϣ��ʾ��"));

        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
                rightPanel);
        centerSplit.setDividerLocation(150);

//        centerSplit2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerSplit,
//                rightPanel2);
//        centerSplit2.setDividerLocation(450);

        northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1, 6));
        northPanel.add(new JLabel("          ��������"));
        northPanel.add(txt_max);
        northPanel.add(new JLabel("           �˿�"));
        northPanel.add(txt_port);
        northPanel.add(btn_start);
        northPanel.add(btn_stop);
        northPanel.setBorder(new TitledBorder("������Ϣ"));

        frame.setLayout(new BorderLayout());
        frame.add(northPanel, "North");
        frame.add(centerSplit, "Center");
        //frame.add(rightPanel2,BorderLayout.EAST);
        frame.add(southPanel, "South");
        frame.setSize(600, 400);
        //frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());//����ȫ��
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((screen_width - frame.getWidth()) / 2,
                (screen_height - frame.getHeight()) / 2);
        frame.setVisible(true);

        // �رմ���ʱ�¼�
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isStart) {
                    closeServer();// �رշ�����
                }
                System.exit(0);// �˳�����
            }
        });

        // �ı��򰴻س���ʱ�¼�
        txt_message.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        // �������Ͱ�ťʱ�¼�
        btn_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                send();
            }
        });

        //�����ļ���ťʱ�¼�
        btn_send_file.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                //�ļ�ѡ��Ի�����������ѡ�����ļ��Ժ��ÿһ��client�����ļ�
                JFileChooser sourceFileChooser = new JFileChooser(".");
                sourceFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int status = sourceFileChooser.showOpenDialog(frame);
                File sourceFile = new File(sourceFileChooser.getSelectedFile().getPath());
                //������text area��ʾ
                contentArea.append("�����ļ���" + sourceFile.getName() + "\r\n");
                for (int i = clients.size() - 1; i >= 0; i--) {
                    SendFileThread sendFile = new SendFileThread(frame, clients.get(i).socket, "������", sourceFileChooser, status);
                    sendFile.start();
                    //client����ʾ
                    clients.get(i).getWriter().println("����������һ���ļ���" + sourceFile.getName() + "(���˷���)");
                    clients.get(i).getWriter().flush();
                }

            }
        });

        // ����������������ťʱ�¼�
        btn_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                if (isStart) {
                    JOptionPane.showMessageDialog(frame, "�������Ѵ�������״̬����Ҫ�ظ�������",
                            "����", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int max;//����
                int port;//�˿ں�
                try {
                    try {
                        max = Integer.parseInt(txt_max.getText());
                    } catch (Exception e1) {
                        throw new Exception("��������Ϊ��������");
                    }
                    if (max <= 0) {
                        throw new Exception("��������Ϊ��������");
                    }
                    try {
                        port = Integer.parseInt(txt_port.getText());
                    } catch (Exception e1) {
                        throw new Exception("�˿ں�Ϊ��������");
                    }
                    if (port <= 0) {
                        throw new Exception("�˿ں� Ϊ��������");
                    }
                    serverStart(max, port);
                    contentArea.append("�������ѳɹ�����!   �������ޣ�" + max + ",  �˿ڣ�" + port
                            + "\r\n");
                    JOptionPane.showMessageDialog(frame, "�������ɹ�����!");
                    btn_start.setEnabled(false);
                    txt_max.setEnabled(false);
                    txt_port.setEnabled(false);
                    btn_stop.setEnabled(true);
                    listModel.addElement("ȫ���û�");
                    user_name_update();
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "����", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // ����ֹͣ��������ťʱ�¼�
        btn_stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isStart) {
                    JOptionPane.showMessageDialog(frame, "��������δ����������ֹͣ��", "����",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    closeServer();
                    btn_start.setEnabled(true);
                    txt_max.setEnabled(true);
                    txt_port.setEnabled(true);
                    btn_stop.setEnabled(false);
                    contentArea.append("�������ɹ�ֹͣ!\r\n");
                    JOptionPane.showMessageDialog(frame, "�������ɹ�ֹͣ��");
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, "ֹͣ�����������쳣��", "����",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    /**
     * �������jlistѡ�������һ���û�
     */
    class SelecTry implements ListSelectionListener {
        int change = 0, who;

        public void valueChanged(ListSelectionEvent e) {
            //System.out.println("you selected:"+listModel.getElementAt(userList.getSelectedIndex()));
            sendfor_who = userList.getSelectedIndex();
        }

    }

    /**
     * �һ�����ģ��
     *
     * @param username
     * @param youxiang
     * @param new_password
     * @return
     */
    public int user_forget(String username, String youxiang, String new_password) {
        try {
            Connection con = null; //����һ��MYSQL���Ӷ���
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
            Statement stmt; //��������
            stmt = con.createStatement();

            String codingpassword = EncoderByMd5(new_password);

            //��ѯ���ݣ���������ͬ���û���
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //ѭ����������
                int userid = selectRes.getInt("Id");
                String username_db = selectRes.getString("username");
                String youxiang_db = selectRes.getString("youxiang");
                if (username.equals(username_db)) {
                    if (youxiang_db.equals(youxiang)) {
                        //����һ������
                        String updateSql = "UPDATE user SET password = '" + codingpassword + "' WHERE Id = " + userid + "";
                        long updateRes = stmt.executeUpdate(updateSql);
                        return 1;
                    }

                }
            }
            return 0;
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }


    /**
     * ע��ģ��
     *
     * @param username
     * @param password
     * @param youxiang
     * @return
     */
    public int user_register(String username, String password, String youxiang) {

        try {
            Connection con = null; //����һ��MYSQL���Ӷ���
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
            Statement stmt; //��������
            stmt = con.createStatement();

            String codingPassword = EncoderByMd5(password);

            //��ѯ���ݣ���������ͬ���û���
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            int id;
            selectRes.last();
            id = selectRes.getRow() + 1;
            while (selectRes.next()) { //ѭ����������
                String username_db = selectRes.getString("username");
                if (username.equals(username_db)) {
                    return 2;
                }
            }
            String friend = null;
            //����һ������
            stmt.executeUpdate("INSERT INTO user (id, username, password,youxiang,friend) VALUES ('" + id + "', '" + username + "', '" + codingPassword + "','" + youxiang + "','" + friend + "')");
            all_listModel.addElement(username);
            return 1;
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }

    /**
     * �����û�����ʱ���ڷ������ı�״̬
     *
     * @param name
     * @return
     */
    public int user_offLine(String name) {
        try {
            Connection con = null; //����һ��MYSQL���Ӷ���
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
            Statement stmt; //��������
            stmt = con.createStatement();

            //
            String username_fromDb;
            int id = 0;
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //ѭ����������
                username_fromDb = selectRes.getString("username");
                id = selectRes.getInt("Id");
                if (name.equals(username_fromDb)) {
                    selectSql = "UPDATE user SET state = 0  WHERE Id = " + id + "";
                    stmt.executeUpdate(selectSql);
                    selectSql = "UPDATE user SET serverPort = 0  WHERE Id = " + id + "";
                    stmt.executeUpdate(selectSql);
                    selectSql = "UPDATE user SET ipAdres = ''  WHERE Id = " + id + "";
                    stmt.executeUpdate(selectSql);
                    return 1;
                }
            }
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }

    /**
     * ��½ģ��
     *
     * @param username
     * @param password
     * @return
     */
    public int user_login(String username, String password,int serverPort,String myIP) {
        try {
            Connection con = null; //����һ��MYSQL���Ӷ���
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
            Statement stmt; //��������
            stmt = con.createStatement();
            String username_fromDb;
            String password_fromDb;
            String codingPassword;
            int state = 0, id = 0;
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //ѭ����������
                username_fromDb = selectRes.getString("username");
                password_fromDb = selectRes.getString("password");
                codingPassword = EncoderByMd5(password);
                id = selectRes.getInt("Id");
                state = selectRes.getInt("state");
                if (username.equals(username_fromDb) && codingPassword.equals(password_fromDb)) {
                    if (state == 0) {
                        selectSql = "UPDATE user SET state = 1  WHERE Id = " + id + "";
                        stmt.executeUpdate(selectSql);
                        selectSql = "UPDATE user SET serverPort = " + serverPort + "  WHERE Id = " + id + "";
                        stmt.executeUpdate(selectSql);
                        selectSql = "UPDATE user SET ipAdres = '" + myIP + "'  WHERE Id = " + id + "";
                        stmt.executeUpdate(selectSql);
                        return 1;//��û�е�½�����Ե�½
                    } else {
                        return 2;//�ѵ�½״̬���޷���½
                    }

                }
            }
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }

    /**
     * ����������
     *
     * @param max
     * @param port
     * @throws java.net.BindException
     */
    public void serverStart(int max, int port) throws java.net.BindException {
        try {
            clients = new ArrayList<ClientThread>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread(serverSocket, max);
            serverThread.start();
            isStart = true;
        } catch (BindException e) {
            isStart = false;
            throw new BindException("�˿ں��ѱ�ռ�ã��뻻һ����");
        } catch (Exception e1) {
            e1.printStackTrace();
            isStart = false;
            throw new BindException("�����������쳣��");
        }
    }

    /**
     * �رշ�����
     */
    @SuppressWarnings("deprecation")
    public void closeServer() {
        try {
            if (serverThread != null)
                serverThread.stop();// ֹͣ�������߳�

            for (int i = clients.size() - 1; i >= 0; i--) {
                // �����������û����͹ر�����
                clients.get(i).getWriter().println("CLOSE");
                clients.get(i).getWriter().flush();
                // �ͷ���Դ
                clients.get(i).stop();// ֹͣ����Ϊ�ͻ��˷�����߳�
                clients.get(i).reader.close();
                clients.get(i).writer.close();
                clients.get(i).socket.close();
                clients.remove(i);
            }
            if (serverSocket != null) {
                serverSocket.close();// �رշ�����������
            }
            listModel.removeAllElements();// ����û��б�
            isStart = false;
            set_user_state_off();
            user_name_update();
        } catch (IOException e) {
            e.printStackTrace();
            isStart = true;
        }
    }

    /**
     * Ⱥ����������Ϣ
     *
     * @param message
     * @param who
     */
    public void sendServerMessage(String message, int who) {
        if (who == 0) {
            StringTokenizer stringTokenizer;
            int flag = 0;
            for (int i = all_listModel.size(); i > 0; i--) {
                flag = 0;
                String msg = all_listModel.getElementAt(i - 1) + "";
                stringTokenizer = new StringTokenizer(
                        msg, "---");
                String user_name = stringTokenizer.nextToken();
                for (int j = clients.size() - 1; j >= 0; j--) {
                    if (user_name.equals(clients.get(j).getUser().getName())) {
                        clients.get(j).getWriter().println("����������˵   " + message);
                        clients.get(j).getWriter().flush();
                        flag = 1;//���û�����״̬���ѷ���ȥ
                        break;
                    }
                }
                if (flag == 0) {
                    //�û�����״̬��������
                    send_messageTo_board("������", user_name, message);
                }
            }
            contentArea.append("��  ȫ���û�   ���ͣ�" + message + "\r\n");
        } else {
            int flag = 0;
            String msg = "" + all_listModel.getElementAt(who);
            StringTokenizer stringTokenizer = new StringTokenizer(
                    msg, "---");
            String user_name = stringTokenizer.nextToken();
            for (int i = clients.size() - 1; i >= 0; i--) {
                if (user_name.equals(clients.get(i).getUser().getName())) {
                    clients.get(i).getWriter().println("����������˵   " + message);
                    clients.get(i).getWriter().flush();
                    flag = 1;//���û�����״̬���ѷ���ȥ
                    break;
                }
            }
            if (flag == 0) {
//                JOptionPane.showMessageDialog(frame, "���û������ߣ��Ѵ�Ϊ���԰壡", "����",
//                        JOptionPane.ERROR_MESSAGE);
                send_messageTo_board("������", user_name, message);
                contentArea.append("��  " + user_name + "  ���ԣ�" + message + "\r\n");
            } else {
                contentArea.append("��  " + user_name + "  ˵��" + message + "\r\n");
            }
        }

    }

    /**
     * �û�������ʱ��������Ϣ���浽����������
     * @param send_from
     * @param send_for
     * @param message
     * @return
     */
    public int send_messageTo_board(String send_from, String send_for, String message) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//�������ڸ�ʽ
        String msg = send_from + "#" + df.format(new Date()) + "#" + message + "#";
        try {
            Connection con = null; //����һ��MYSQL���Ӷ���
            Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
            Statement stmt; //��������
            stmt = con.createStatement();

            //��ѯ���ݣ���������ͬ���û���
            String selectSql = "SELECT * FROM user";
            ResultSet selectRes = stmt.executeQuery(selectSql);
            while (selectRes.next()) { //ѭ����������
                int Id = selectRes.getInt("Id");
                String username_db = selectRes.getString("username");
                if (send_for.equals(username_db)) {
                    String old_message = selectRes.getString("message");
                    String updateSql = "UPDATE user SET message = '" + old_message + msg + "' WHERE Id = " + Id + "";
                    stmt.executeUpdate(updateSql);
                    return 1;
                }
            }
            return 0;
        } catch (Exception e) {
            System.out.print("MYSQL ERROR:" + e.getMessage());
        }
        return 0;
    }

    /**
     * �������߳�
     */
    class ServerThread extends Thread {
        private ServerSocket serverSocket;
        private int max;// ��������

        // �������̵߳Ĺ��췽��
        public ServerThread(ServerSocket serverSocket, int max) {
            this.serverSocket = serverSocket;
            this.max = max;
        }

        public void run() {
            while (true) {// ��ͣ�ĵȴ��ͻ��˵�����
                try {
                    Socket socket = serverSocket.accept();
                    if (clients.size() == max) {// ����Ѵ���������
                        BufferedReader r = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        PrintWriter w = new PrintWriter(socket
                                .getOutputStream());
                        // ���տͻ��˵Ļ����û���Ϣ
                        String inf = r.readLine();
                        StringTokenizer st = new StringTokenizer(inf, "#");
                        User user = new User(st.nextToken(), st.nextToken());
                        // �������ӳɹ���Ϣ
                        w.println("MAX#���������Բ���" + user.getName()
                                + user.getIp() + "�����������������Ѵ����ޣ����Ժ������ӣ�");
                        w.flush();
                        // �ͷ���Դ
                        r.close();
                        w.close();
                        socket.close();
                        continue;
                    }
                    ClientThread client = new ClientThread(socket);
                    client.start();// �����Դ˿ͻ��˷�����߳�
                    client.getUser().setState(1);//����״̬
                    clients.add(client);
                    listModel.addElement(client.getUser().getName());// ���������б�
                    contentArea.append(client.getUser().getName()
                            + client.getUser().getIp() + "����!\r\n");
//                    user_name_update();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Ϊһ���ͻ��˷�����߳�
     */
    class ClientThread extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private User user;

        public BufferedReader getReader() {
            return reader;
        }

        public PrintWriter getWriter() {
            return writer;
        }

        public User getUser() {
            return user;
        }

        // �ͻ����̵߳Ĺ��췽��
        public ClientThread(Socket socket) {
            try {
                this.socket = socket;
                reader = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
                writer = new PrintWriter(socket.getOutputStream());
                // ���տͻ��˵Ļ����û���Ϣ
                String inf = reader.readLine();
                StringTokenizer st = new StringTokenizer(inf, "#");
                user = new User(st.nextToken(), st.nextToken());
                // �������ӳɹ���Ϣ
                writer.println(user.getName() + user.getIp() + "����������ӳɹ�!");
                writer.flush();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        public void run() {// ���Ͻ��տͻ��˵���Ϣ�����д���
            String message = null;
            while (true) {
                try {
                    message = reader.readLine();// ���տͻ�����Ϣ
                    StringTokenizer stringTokenizer = new StringTokenizer(
                            message, "#");
                    String command = stringTokenizer.nextToken();// ����
                    if (command.equals("CLOSE"))// ��������
                    {
                        contentArea.append(this.getUser().getName()
                                + this.getUser().getIp() + "����!\r\n");
                        // �Ͽ������ͷ���Դ
                        user_offLine(this.getUser().getName());
                        this.getUser().setState(0);
                        reader.close();
                        writer.close();
                        socket.close();

                        user_name_update();//�����û�״̬


                        //�����û�״̬
                        String liststr = "";
                        for (int j = 1; j < all_listModel.size(); j++) {
                            liststr += all_listModel.get(j) + "#";
                        }
                        // �����������û����͸��û���������
                        for (int j = clients.size()-1 ; j >= 0; j--) {
                            clients.get(j).getWriter().println(
                                    "USERLIST#" + all_listModel.size() + "#" + liststr);
                            clients.get(j).getWriter().flush();
                        }
                        user_name_update();//�����û�״̬

                        listModel.removeElement(user.getName());// ���������б�

                        // ɾ�������ͻ��˷����߳�
                        for (int i = clients.size() - 1; i >= 0; i--) {
                            if (clients.get(i).getUser() == user) {
                                ClientThread temp = clients.get(i);
                                clients.remove(i);// ɾ�����û��ķ����߳�
                                temp.stop();// ֹͣ���������߳�
                                return;
                            }
                        }
                    } else if (command.equals("USERLOGIN")) {
                        String username = stringTokenizer.nextToken();
                        String password = stringTokenizer.nextToken();
                        int serverPort = Integer.parseInt(stringTokenizer.nextToken());
                        String myIP = stringTokenizer.nextToken();
                        int i = user_login(username, password,serverPort,myIP);
                        if (1 == i) {
                            user_name_update();
                            String msg = get_message(username);
                            if (msg == null) {
                                writer.println("USERLOGIN#OK#");
                                writer.flush();
                            } else {
                                writer.println("USERLOGIN#OK#" + msg);
                                writer.flush();
                            }


                            //�����û�״̬
                            String temp = "";
                            for (int j = 1; j < all_listModel.size(); j++) {
                                temp += all_listModel.get(j) + "#";
                            }
                            // �����������û����͸��û���������
                            for (int j = clients.size()-1 ; j >= 0; j--) {
                                clients.get(j).getWriter().println(
                                        "USERLIST#" + all_listModel.size() + "#" + temp);
                                clients.get(j).getWriter().flush();
                            }

                        } else if (2 == i) {
                            writer.println("USERLOGIN#ALREADY");
                            writer.flush();
                        } else {
                            writer.println("USERLOGIN#NO");
                            writer.flush();

                        }
                        user_name_update();
                    } else if (command.equals("USERZHUCE")) {
                        String username = stringTokenizer.nextToken();
                        String password = stringTokenizer.nextToken();
                        String youxiang = stringTokenizer.nextToken();
                        int i = user_register(username, password, youxiang);
                        if (1 == i) {
                            writer.println("USERZHUCE#OK");
                            writer.flush();
                            contentArea.append("�����û�ע�ᣡ     �û�����" + username + "\r\n");
                            user_name_update();//�����û�״̬
                        } else if (i == 2) {
                            writer.println("USERZHUCE#exict");
                            writer.flush();
                        } else {
                            writer.println("USERZHUCE#NO");
                            writer.flush();
                        }
                    } else if (command.equals("USERFORGET")) {
                        String username = stringTokenizer.nextToken();
                        String youxiang = stringTokenizer.nextToken();
                        String new_password = stringTokenizer.nextToken();
                        int i = user_forget(username, youxiang, new_password);
                        if (1 == i) {
                            //JOptionPane.showMessageDialog(frame, "��½�ɹ�!" );
                            writer.println("USERFORGET#OK");
                            writer.flush();
                            contentArea.append("   �û���" + username + "  �޸����룡\r\n");
                        } else if (i == 2) {
                            writer.println("USERFORGET#YOUXIANG_WRONG");
                            writer.flush();
                        } else if (i == 3) {
                            writer.println("USERFORGET#NAME_NO_exict");
                            writer.flush();
                        } else {
                            writer.println("USERFORGET#NO");
                            writer.flush();
                        }
                    } else if (command.equals("P2P")) {
                        String username = stringTokenizer.nextToken();
                        int i = get_user_serverPort(username);
                        String ip = get_user_serverIP(username);
                        if(i!=0){
                            writer.println("P2P#OK#"+username+"#"+i+"#"+ip);
                            writer.flush();
                        }else{
                            writer.println("P2P#NO#"+username);
                            writer.flush();
                        }
                    }else if(command.equals("LIXIAN")){
                        String username_sent = stringTokenizer.nextToken();
                        String username_receive = stringTokenizer.nextToken();
                        String msg = stringTokenizer.nextToken();
                        send_messageTo_board(username_sent,username_receive,msg);
                        System.out.println("���߷���ok");
                    } else {
                        dispatcherMessage(message);// ת����Ϣ
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public int get_user_serverPort(String user_name){
            try {
                Connection con = null; //����һ��MYSQL���Ӷ���
                Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
                con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
                Statement stmt; //��������
                stmt = con.createStatement();

                //��ѯ���ݣ���������ͬ���û���
                String selectSql = "SELECT * FROM user";
                ResultSet selectRes = stmt.executeQuery(selectSql);
                while (selectRes.next()) { //ѭ����������
                    String username_db = selectRes.getString("username");
                    if (user_name.equals(username_db)) {
                        int serverPort = selectRes.getInt("serverPort");
                        return serverPort;
                    }
                }
            } catch (Exception e) {
                System.out.print("MYSQL ERROR:" + e.getMessage());
            }
            return 0;
        }

        public String get_user_serverIP(String user_name){
            try {
                Connection con = null; //����һ��MYSQL���Ӷ���
                Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
                con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
                Statement stmt; //��������
                stmt = con.createStatement();

                //��ѯ���ݣ���������ͬ���û���
                String selectSql = "SELECT * FROM user";
                ResultSet selectRes = stmt.executeQuery(selectSql);
                while (selectRes.next()) { //ѭ����������
                    String username_db = selectRes.getString("username");
                    if (user_name.equals(username_db)) {
                        String serverIP = selectRes.getString("ipAdres");
                        return serverIP;
                    }
                }
            } catch (Exception e) {
                System.out.print("MYSQL ERROR:" + e.getMessage());
            }
            return "";
        }

        public String get_message(String name) {
            try {
                Connection con = null; //����һ��MYSQL���Ӷ���
                Class.forName("com.mysql.jdbc.Driver").newInstance();//MYSQL����
                con = DriverManager.getConnection("jdbc:mysql://localhost:3306/server_db?&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "321321"); //���ӱ���MYSQL
                Statement stmt; //��������
                stmt = con.createStatement();

                //��ѯ���ݣ���������ͬ���û���
                String selectSql = "SELECT * FROM user";
                ResultSet selectRes = stmt.executeQuery(selectSql);
                while (selectRes.next()) { //ѭ����������
                    int Id = selectRes.getInt("Id");
                    String username_db = selectRes.getString("username");
                    if (name.equals(username_db)) {
                        String message = selectRes.getString("message");
                        String updateSql = "UPDATE user SET message = '' WHERE Id = " + Id + "";
                        stmt.executeUpdate(updateSql);
                        return message;
                    }
                }
                return "";
            } catch (Exception e) {
                System.out.print("MYSQL ERROR:" + e.getMessage());
            }
            return "";
        }

        // ת����Ϣ
        public void dispatcherMessage(String message) {
            StringTokenizer stringTokenizer = new StringTokenizer(message, "#");
            String source = stringTokenizer.nextToken();
            String owner = stringTokenizer.nextToken();
            String content = stringTokenizer.nextToken();

            if (owner.equals("ALL")) {// Ⱥ��
                message = source + "˵��" + content;
                contentArea.append(message + "\r\n");
                for (int i = clients.size() - 1; i >= 0; i--) {
                    clients.get(i).getWriter().println(message + "(���˷���)");
                    clients.get(i).getWriter().flush();
                }
            } else {
                for (int i = clients.size() - 1; i >= 0; i--) {
                    if (clients.get(i).user.getName().equals(owner)) {
                        clients.get(i).getWriter().println(owner + "  ����˵: " + content);
                        clients.get(i).getWriter().flush();
                        //contentArea.append(owner+"  ��    "+ clients.get(i).user.getName()+ "  ˵  :"+ content+"\r\n");
                    }
                    if (clients.get(i).user.getName().equals(source)) {
                        clients.get(i).getWriter().println("��   " + source + "  ˵: " + content);
                        clients.get(i).getWriter().flush();
                    }
                }
            }
        }
    }
}