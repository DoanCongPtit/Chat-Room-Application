/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat.application;

import static chat.application.Server.fileId;
import static chat.application.Server.listModelFile;
import static chat.application.Server.listOnl;
import static chat.application.Server.myFiles;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author doanc
 */
public class Client extends javax.swing.JFrame {

    int port = 3333;
    Socket socket;
    static DefaultListModel<String> listModelUser;
    final File[] fileToSend = new File[1];
    static DefaultListModel<String> listModelFile;
    static ArrayList<MyFile> myFiles;
    static int fileId = 0;

    public void Execute() throws IOException {
        socket = new Socket(InetAddress.getLocalHost(), port);
        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        dout.writeUTF(NameText.getText());
        new ReadClient(socket).start();
    }

    /**
     * Creates new form Client
     */
    public Client() throws UnknownHostException, IOException {
        initComponents();
    }

    class ReadClient extends Thread {

        Socket socket;

        public ReadClient(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            DataInputStream din = null;
            try {

                din = new DataInputStream(socket.getInputStream());

                while (true) {
                    String msg = din.readUTF();
                    msg = msg.trim();
                    if (msg.contains("file")) {
                        //msg = formatString(msg);
                        MsgBox.append(msg + "\n");
                        System.out.println("msg " + msg);
                        int fileNameLength = din.readInt();
                        System.out.println("//////////////" + fileNameLength);
                        byte[] fileNameBytes = new byte[fileNameLength];
                        // Read from the input stream into the byte array.
                        din.readFully(fileNameBytes, 0, fileNameBytes.length);
                        // Create the file name from the byte array.
                        String fileName = new String(fileNameBytes);
                        //String fileNameBytes = din.readUTF();
                        System.out.println("filename " + fileName);
                        // Read how much data to expect for the actual content of the file.
                        int fileContentLength = din.readInt();
//                        // If the file exists.
                        if (fileContentLength > 0) {
//                            // Array to hold the file data.
                            byte[] fileContentBytes = new byte[fileContentLength];
//                            // Read from the input stream into the fileContentBytes array.
                            din.readFully(fileContentBytes, 0, fileContentBytes.length);
//
                            myFiles.add(new MyFile(fileId, fileName, fileContentBytes));
                            fileId++;

                            listModelFile.addElement(fileName);
                            jListFile.setModel(listModelFile);
                            //}
                        }
                    } else {
                        boolean listCheck = false;
                        if (msg.contains("list")) {
                            listModelUser.removeAllElements();
                            String[] arrOfUser = msg.split(",");
                            for (String i : arrOfUser) {
                                if (!i.equals("list")) {
                                    if (!i.equals(NameText.getText())) {
                                        listModelUser.addElement(i);
                                    }
                                }
                            }
                            jlistUser.setModel(listModelUser);
                            listCheck = true;
                        }
                        if (listCheck != true) {
                            //msg = formatString(msg);
                            MsgBox.append(msg + "\n");
                            System.out.println(msg);
                        }
                    }

                }

            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public String formatString(String msg) {
            msg = msg.replace(">", " ");
            StringBuilder sb = new StringBuilder(msg);
            sb.insert(1, " to");

            msg = sb.toString();
            return msg;
        }
    }

    class WriteClient extends Thread {

        Socket socket;

        public WriteClient(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            DataOutputStream dout = null;
            try {
                dout = new DataOutputStream(socket.getOutputStream());
                if (SendText.getText().contains("File:")) {
                    //System.out.println("File");
                    FileInputStream fileInputStream = new FileInputStream(fileToSend[0].getAbsolutePath());
                    // Get the name of the file you want to send and store it in filename.
                    String fileName = fileToSend[0].getName();
                    // Convert the name of the file into an array of bytes to be sent to the server.
                    byte[] fileNameBytes = fileName.getBytes();
                    // Create a byte array the size of the file so don't send too little or too much data to the server.
                    byte[] fileBytes = new byte[(int) fileToSend[0].length()];
                    // Put the contents of the file into the array of bytes to be sent so these bytes can be sent to the server.
                    fileInputStream.read(fileBytes);

                    String partner = "all";
                    if (jlistUser.getSelectedIndex() != -1) {
                        partner = jlistUser.getSelectedValue();
                    }
                    String msg = NameText.getText() + ">" + partner + "> file: " + fileName;
                    dout.writeUTF(msg);
                    // Send the length of the name of the file so server knows when to stop reading.
                    dout.writeInt(fileNameBytes.length);
                    // Send the file name.
                    dout.write(fileNameBytes);
                    // Send the length of the byte array so the server knows when to stop reading.
                    dout.writeInt(fileBytes.length);
                    // Send the actual file.
                    dout.write(fileBytes);
                    SendText.setText("");
                    jListFile.clearSelection();
                    MsgBox.append(msg + "\n");
                } else {
                    String partner = "all";
                    if (jlistUser.getSelectedIndex() != -1) {
                        partner = jlistUser.getSelectedValue();
                    }

                    String msg = SendText.getText();
                    String name = NameText.getText();

                    String date = getTime();

                    dout.writeUTF(name + ">" + partner + "> [" + date + "] " + ": " + msg);
                    if (!partner.equals("all")) {
                        MsgBox.append(name + " to " + partner + " [" + date + "] " + ": " + msg + "\n");
                    }
                    SendText.setText("");
                }

            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        public String getTime() {
            LocalDateTime now = LocalDateTime.now();
            int hour = now.getHour();
            int minute = now.getMinute();
            int second = now.getSecond();
            String date = hour + "-" + minute + "-" + second;
            return date;
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

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        NameText = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        MsgBox = new javax.swing.JTextArea();
        SendText = new javax.swing.JTextField();
        connectButton = new javax.swing.JToggleButton();
        SendButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jlistUser = new javax.swing.JList<>();
        jButtonFile = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jListFile = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel1.setText("CLIENT");

        jLabel2.setText("Name : ");

        MsgBox.setEditable(false);
        MsgBox.setColumns(20);
        MsgBox.setRows(5);
        MsgBox.setHighlighter(null);
        jScrollPane1.setViewportView(MsgBox);

        connectButton.setText("Connect");
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        SendButton.setText("Send");
        SendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SendButtonActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel3.setText("User online");

        jScrollPane2.setViewportView(jlistUser);

        jButtonFile.setText("File");
        jButtonFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFileActionPerformed(evt);
            }
        });

        jLabel4.setText("File");

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
                        .addComponent(SendText, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(SendButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonFile))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(157, 157, 157)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(connectButton)
                                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 333, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(NameText, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(44, 44, 44)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(36, 36, 36)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(NameText, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(connectButton)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(34, 34, 34)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SendText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SendButton)
                    .addComponent(jButtonFile))
                .addContainerGap(31, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void SendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SendButtonActionPerformed
        new WriteClient(socket).start();
    }//GEN-LAST:event_SendButtonActionPerformed

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        try {
            if (connectButton.isSelected()) {
                listModelUser = new DefaultListModel<>();
                listModelFile = new DefaultListModel<>();
                myFiles = new ArrayList<>();
                Execute();
                connectButton.setText("connected");
                NameText.setEditable(false);
            } else {
                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                dout.writeUTF(NameText.getText() + " : exit");
                connectButton.setText("connect");
                NameText.setEditable(true);
                socket.close();
                dout.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_connectButtonActionPerformed

    private void jButtonFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFileActionPerformed
        JFileChooser jFileChooser = new JFileChooser();
        // Set the title of the dialog.
        jFileChooser.setDialogTitle("Choose a file to send.");
        // Show the dialog and if a file is chosen from the file chooser execute the following statements.
        if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            // Get the selected file.
            fileToSend[0] = jFileChooser.getSelectedFile();
            // Change the text of the java swing label to have the file name.
            String partner = "all";
            if (jlistUser.getSelectedIndex() != -1) {
                partner = jlistUser.getSelectedValue();
            }
            SendText.setText(NameText.getText() + ">" + partner + "> File: " + fileToSend[0].getName());

        }
    }//GEN-LAST:event_jButtonFileActionPerformed

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
        // TODO add your handling code here:
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
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Client().setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea MsgBox;
    private javax.swing.JTextField NameText;
    private javax.swing.JButton SendButton;
    private javax.swing.JTextField SendText;
    private javax.swing.JToggleButton connectButton;
    private javax.swing.JButton jButtonFile;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JList<String> jListFile;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JList<String> jlistUser;
    // End of variables declaration//GEN-END:variables
}
