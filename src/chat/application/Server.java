/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat.application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

/**
 *
 * @author doanc
 */
public class Server extends javax.swing.JFrame {

    static int port = 3333;
    static Map<Socket, String> listOnl;
    static DefaultListModel<String> listModelUser;
    static DefaultListModel<String> listModelFile;
    static ArrayList<MyFile> myFiles;
    static int fileId = 0;

    /**
     * Creates new form Server
     */
    class StartServer extends Thread {

        public void run() {
            try {
                ServerSocket sSocket = new ServerSocket(Server.port);
                System.out.println("running....");
                StatusText.setText("Running");
                RunButton.setText("running");

                while (true) {
                    try {
                        Socket socket = sSocket.accept();
                        DataInputStream din = new DataInputStream(socket.getInputStream());
                        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                        String name = din.readUTF();
                        System.out.println(name + " joined " + socket.getPort());
                        MsgBox.append(name + " joined \n");
                        //listUser.add(socket);
                        initListUserBoard(socket, name);
                        String arrOfUser = getListUser();
                        arrOfUser += "list";
                        sendMulticast(dout, arrOfUser);
                        new BroadcastServer(socket).start();

                    } catch (IOException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void initListUserBoard(Socket socket, String name) {
            listOnl.put(socket, name);
            listModelUser.addElement(name);
            jlistUserJoin.setModel(listModelUser);
        }

    }

    public static String getListUser() {
        String arrOfUser = "";
        for (Socket i : listOnl.keySet()) {
            arrOfUser += listOnl.get(i) + ",";
        }
        return arrOfUser;
    }

    public static void sendMulticast(DataOutputStream dout, String msg) throws IOException {
        Set<Socket> keySet = listOnl.keySet();
        for (Socket i : keySet) {
            dout = new DataOutputStream(i.getOutputStream());
            dout.writeUTF(msg + "\n");
        }
    }

    public Server() {
        initComponents();
    }

    class BroadcastServer extends Thread {

        Socket socket;

        public BroadcastServer(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            DataInputStream din = null;
            DataOutputStream dout = null;
            try {
                while (true) {
                    din = new DataInputStream(socket.getInputStream());
                    String msg = din.readUTF();
                    System.out.println("msg " + msg);
                    if (msg.contains("file")) {
                        int fileNameLength = din.readInt();
                        byte[] fileNameBytes = new byte[fileNameLength];
                        // Read from the input stream into the byte array.
                        din.readFully(fileNameBytes, 0, fileNameBytes.length);
                        // Create the file name from the byte array.
                        String fileName = new String(fileNameBytes);
                        //String fileNameBytes = din.readUTF();

                        // Read how much data to expect for the actual content of the file.
                        int fileContentLength = din.readInt();
                        // If the file exists.
                        String partner = getPartner(msg);

                        msg = listOnl.get(socket) + ">" + partner + "> file: " + fileName;
                        System.out.println("msg 2 " + msg);
                        if (fileContentLength > 0) {
                            // Array to hold the file data.
                            byte[] fileContentBytes = new byte[fileContentLength];
                            // Read from the input stream into the fileContentBytes array.
                            din.readFully(fileContentBytes, 0, fileContentBytes.length);
                            MyFile file = new MyFile(fileId, fileName, fileContentBytes);
                            myFiles.add(file);
                            fileId++;

                            listModelFile.addElement(fileName);
                            jListFile.setModel(listModelFile);

                            if (!partner.equals("all")) {
                                Set<Socket> keySet = listOnl.keySet();
                                for (Socket i : keySet) {

                                    if (listOnl.get(i).equals(partner)) {
                                        dout = new DataOutputStream(i.getOutputStream());
                                        dout.writeUTF(msg);
                                        System.out.println("length " + fileNameBytes.length);
                                        // Send the length of the name of the file so server knows when to stop reading.
                                        dout.writeInt(fileNameBytes.length);
                                        // Send the file name.
                                        dout.write(fileNameBytes);
//                                        // Send the length of the byte array so the server knows when to stop reading.
                                        dout.writeInt(fileContentLength);
//                                        // Send the actual file.
                                        dout.write(file.getData());
                                    }
                                }
                            } else {
                                //sendMulticast(dout, msg);
                                Set<Socket> keySet = listOnl.keySet();
                                for (Socket i : keySet) {
                                    dout = new DataOutputStream(i.getOutputStream());
                                    dout.writeUTF(msg);
                                    System.out.println("length " + fileNameBytes.length);
                                    // Send the length of the name of the file so server knows when to stop reading.
                                    dout.writeInt(fileNameBytes.length);
                                    // Send the file name.
                                    dout.write(fileNameBytes);
                                    // Send the length of the byte array so the server knows when to stop reading.
                                    dout.writeInt(fileContentLength);
                                    // Send the actual file.
                                    dout.write(file.getData());

                                }
                            }
                            MsgBox.append(msg + "\n");
                        }
                    } else {
                        String arrOfUser = getListUser();
                        arrOfUser += "list";
                        sendMulticast(dout, arrOfUser);

                        if (msg.contains("exit")) {
                            String[] arrClients = msg.split(" : ");
                            removeUser(socket, arrClients[0]);
                            msg = arrClients[0] + " disconnected";
                            arrOfUser = getListUser();
                            arrOfUser += "list";
                            sendMulticast(dout, arrOfUser);
                            socket.close();
                        }

                        String partner = getPartner(msg);

                        if (!partner.equals("all")) {
                            Set<Socket> keySet = listOnl.keySet();
                            for (Socket i : keySet) {
                                dout = new DataOutputStream(i.getOutputStream());
                                if (listOnl.get(i).equals(partner)) {
                                    System.out.println("partner2 " + partner);
                                    dout.writeUTF(msg + "\n");
                                }
                            }
                        } else {
                            sendMulticast(dout, msg);
                        }
                        MsgBox.append(msg + "\n");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void removeUser(Socket socket, String name) {
            listOnl.remove(socket);
            for (int i = 0; i < listModelUser.size(); i++) {
                if (listModelUser.get(i).equals(name)) {
                    listModelUser.remove(i);
                    System.out.println(listModelUser.get(i));
                }
            }
            jlistUserJoin.setModel(listModelUser);
            System.out.println(name + " disconnected");

        }

        public String getPartner(String msg) {
            String partner = msg.substring(msg.indexOf(">") + 1);
            partner = partner.substring(0, partner.indexOf(">"));
            return partner;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jlistUserJoin = new javax.swing.JList<>();
        jLabel3 = new javax.swing.JLabel();
        RunButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        MsgBox = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        StatusText = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jListFile = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        jScrollPane2.setViewportView(jlistUserJoin);

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel3.setText("User joined");

        RunButton.setText("RUN");
        RunButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RunButtonActionPerformed(evt);
            }
        });

        MsgBox.setEditable(false);
        MsgBox.setColumns(20);
        MsgBox.setRows(5);
        jScrollPane1.setViewportView(MsgBox);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel1.setText("SERVER");

        StatusText.setText(".................");

        jLabel2.setText("STATUS : ");

        jLabel4.setText("Files");

        jListFile.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jListFileMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(jListFile);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(163, 163, 163)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(StatusText, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(RunButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 310, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(40, 40, 40)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(StatusText)))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(RunButton)
                            .addComponent(jLabel3))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(43, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void RunButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunButtonActionPerformed
        //listUser = new ArrayList<>();

        listModelUser = new DefaultListModel<>();
        listModelFile = new DefaultListModel<>();
        listOnl = new HashMap<>();
        myFiles = new ArrayList<>();
        new StartServer().start();

    }//GEN-LAST:event_RunButtonActionPerformed

    private void jListFileMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jListFileMouseClicked
        String fileName = jListFile.getSelectedValue();
        FileOutputStream fileOutputStream = null;
        for (MyFile i : myFiles) {
            if (i.getName().equals(fileName)) {
                try {
                    // Create a stream to write data to the file.
                    fileOutputStream = new FileOutputStream("D:\\Documents\\" + fileName);
                    // Write the actual file data to the file.
                    fileOutputStream.write(i.getData());

                    // Close the stream.
                    fileOutputStream.close();
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        JOptionPane.showMessageDialog(rootPane, "File downloaded at D:\\Documents\\" + fileName);
    }//GEN-LAST:event_jListFileMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Server().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea MsgBox;
    private javax.swing.JButton RunButton;
    private javax.swing.JLabel StatusText;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JList<String> jListFile;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JList<String> jlistUserJoin;
    // End of variables declaration//GEN-END:variables

}
