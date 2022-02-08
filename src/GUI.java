import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import java.sql.*;

public class GUI extends JFrame {

    // Creating labels for the frame
    private JLabel labelDriver = new JLabel("JDBC Driver");;
    private JLabel labelDatabase = new JLabel("Database URL");
    private JLabel labelUsername = new JLabel("Username");
    private JLabel labelPassword = new JLabel("Password");
    private JLabel labelStatus = new JLabel("No Connection Now");

    // Creating menus for the dropdowns
    String [] driverOptions = { "com.mysql.cj.jdbc.Driver", "" };
    String [] databaseOptions = { "jdbc:mysql://localhost:3306/test", "" };

    // Creating dropdows for the frame
    private JComboBox driverList = new JComboBox(driverOptions);
    private JComboBox databaseList = new JComboBox(databaseOptions);

    // Creating text fields for the frame
    private JTextField textUsername = new JTextField();
    private JPasswordField textPassword = new JPasswordField();

    // Creating text area for the frame
    private JTextArea commandBox = new JTextArea(3, 75);

    // Creating buttons for the frame
    private JButton buttonConnect = new JButton("Connect to Database");
    private JButton buttonClearCommand = new JButton("Clear SQL Command");
    private JButton buttonExecute = new JButton("Execute SQL Command");
    private JButton buttonClearResult = new JButton("Clear Result Window");

    // Creating tables for the frame
    private ResultSetTableModel tableModel;
    private JTable table = new JTable();

    // Creating objects to establish and update connection
    private Connection connection;
    private boolean connectedToDatabase = false;

    // Setting up all of the gui components
    public GUI() throws ClassNotFoundException, SQLException, IOException {
        this.labelStatus.setForeground(Color.RED);
        this.driverList.setSelectedIndex(0);
        this.commandBox.setWrapStyleWord(true);
        this.commandBox.setLineWrap(true);

        this.eventHandler();
    }

    public void eventHandler() {
        // Establishes a connection
        this.buttonConnect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                try {
                    Class.forName(String.valueOf(driverList.getSelectedItem()));
                    connection = DriverManager.getConnection(String.valueOf(databaseList.getSelectedItem()), textUsername.getText(), textPassword.getText());

                    labelStatus.setText("Connected to " + String.valueOf(databaseList.getSelectedItem()));
                    labelStatus.setForeground(new Color(0,100,0));//Color.GREEN);
                    connectedToDatabase = true;

                } catch (Exception e) {

                    labelStatus.setText("Unable to connect");
                    labelStatus.setForeground(Color.RED);
                    connectedToDatabase = false;
                    e.printStackTrace();

                    table.setModel(new DefaultTableModel());
                    tableModel = null;
                }
            }
        });


        // "Clears sql command box
        this.buttonClearCommand.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                commandBox.setText("");
            }
        });

        // Run commands given by the user
        this.buttonExecute.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                if(connectedToDatabase == true) {

                    // Captures the command written by the user
                    boolean flag = false;
                    String command = commandBox.getText();

                    // First command executed will create a new table modal and execute the inital command
                    if (tableModel == null) {

                        try {

                            tableModel = new ResultSetTableModel(command, connection);
                            table.setModel(tableModel);
                            flag = true;

                        } catch (ClassNotFoundException | SQLException | IOException e) {

                            JOptionPane.showMessageDialog( null, e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE );
                            e.printStackTrace();
                        }

                        if (flag) {

                            try {
                                // Connect to log database
                                Connection logConnection = logConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/operationslog", "root", "root");
                                Statement statement = logConnection.createStatement();
                                statement.executeUpdate("UPDATE operationscount SET num_queries = num_queries + 1");

                                statement.close();
                                logConnection.close();

                            } catch(Exception e) {
                                e.printStackTrace();
                            }

                            flag = false;
                        }
                    }

                    // Statement that will update the table model if already exists
                    else {

                        if(command.contains("select") || command.contains("SELECT")) {
                            try {

                                tableModel.setQuery(command);
                                flag = true;

                            } catch (IllegalStateException | SQLException e) {

                                table.setModel(new DefaultTableModel());
                                tableModel = null;

                                JOptionPane.showMessageDialog( null, e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE );
                                e.printStackTrace();
                            }

                            if (flag) {
                                try {
                                    // Connect to log database
                                    Connection logConnection = logConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/operationslog", "root", "root");
                                    Statement statement = logConnection.createStatement();
                                    statement.executeUpdate("UPDATE operationscount SET num_queries = num_queries + 1");

                                    statement.close();
                                    logConnection.close();

                                } catch(Exception e) {
                                    e.printStackTrace();
                                }

                                flag = false;
                            }
                        }

                        // If we don't have a select command we will reset input from result window
                        else {

                            try {
                                tableModel.setUpdate(command);

                                table.setModel(new DefaultTableModel());
                                tableModel = null;
                                flag = true;

                            } catch (IllegalStateException | SQLException e) {

                                table.setModel(new DefaultTableModel());
                                tableModel = null;

                                JOptionPane.showMessageDialog( null, e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE );
                                e.printStackTrace();
                            }

                            if (flag) {
                                try {
                                    // Connect to log database
                                    Connection logConnection = logConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/operationslog", "root", "root");
                                    Statement statement = logConnection.createStatement();
                                    statement.executeUpdate("UPDATE operationscount SET num_updates = num_updates + 1");

                                    statement.close();
                                    logConnection.close();

                                } catch(Exception e) {
                                    e.printStackTrace();
                                }

                                flag = false;
                            }
                        }
                    }
                }
            }
        });

        // "Clear Result" button handler
        this.buttonClearResult.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                table.setModel(new DefaultTableModel());
                tableModel = null;
            }
        });

        // panel for the buttons
        JPanel buttons = new JPanel(new GridLayout(1, 4));
        buttons.add(this.labelStatus);
        buttons.add(this.buttonConnect);
        buttons.add(this.buttonClearCommand);
        buttons.add(this.buttonExecute);

        // panels for textfields and tables
        JPanel labelsAndTextFields = new JPanel(new GridLayout(4, 2));
        labelsAndTextFields.add(this.labelDriver);
        labelsAndTextFields.add(this.driverList);
        labelsAndTextFields.add(this.labelDatabase);
        labelsAndTextFields.add(this.databaseList);
        labelsAndTextFields.add(this.labelUsername);
        labelsAndTextFields.add(this.textUsername);
        labelsAndTextFields.add(this.labelPassword);
        labelsAndTextFields.add(this.textPassword);


        // panel for the top of the gui (jlb/jtf and jta)
        JPanel top = new JPanel(new GridLayout(1, 2));
        top.add(labelsAndTextFields);
        top.add(this.commandBox);

        //panel for table and buttton
        JPanel south = new JPanel();
        south.setLayout(new BorderLayout(20,0));
        south.add(new JScrollPane(this.table), BorderLayout.NORTH);
        south.add(this.buttonClearResult, BorderLayout.SOUTH);

        // add panels to frame
        add(top, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);


        // dispose of window when user quits application (this overrides
        // the default of HIDE_ON_CLOSE)
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );

        // ensure database connection is closed when user quits application
        addWindowListener(new WindowAdapter() {
            // disconnect from database and exit when window has closed
            public void windowClosed( WindowEvent event ) {

                try {
                    //close connection on frame exit
                    if(!connection.isClosed())
                        connection.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                System.exit( 0 );
            } // end method windowClosed
        });
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

        // Setting the displey config of the program
        GUI gui = new GUI();
        gui.pack();
        gui.setVisible(true);
        gui.setLocationRelativeTo(null);
    }
}
