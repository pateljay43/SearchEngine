/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author JAY
 */
public class GUI extends JFrame {

    private final QueryProcessor queryProcessor;
    private final QuerySyntaxCheck syntaxChecker;
    private final ArrayList<String> fileNames;
    private static HashMap<String, List<Integer>> queryHistory;
//    private static int queryHistoryPointer;
    private List<Integer> queryResult;

    private final JTextField queryTF;
    private final JButton searchBtn;
    // result table
    private final TableModel tableModel;
    private final JTable Jtable;
    private final JScrollPane tableScrollPane;
    private final JLabel processingTimeLBL;
    private final JLabel processingTime;
    private final JLabel indexStatisticsLBL;

    public GUI(QueryProcessor _queryProcessor, ArrayList<String> _fileNames) {
        queryProcessor = _queryProcessor;
        fileNames = _fileNames;
        syntaxChecker = new QuerySyntaxCheck();
        queryResult = new ArrayList<>();
        queryHistory = new HashMap<>();
//        queryHistoryPointer = queryHistory.size();

        queryTF = new JTextField();
        queryTF.setBounds(10, 5, 700, 40);
        queryTF.addKeyListener(new KeyListenerImpl());
        add(queryTF);

        searchBtn = new JButton("Search");
        searchBtn.setBounds(710, 10, 80, 30);
        searchBtn.addActionListener(new ActionListenerImpl());
        add(searchBtn);

        indexStatisticsLBL = new JLabel("Index Statistics:-  Press (Ctrl + i)");
        indexStatisticsLBL.setBounds(15, 45, 250, 30);
        add(indexStatisticsLBL);

        tableModel = new TableModel();
        Jtable = new JTable(tableModel);
        Jtable.setGridColor(Color.gray);
        Jtable.setShowVerticalLines(false);
        tableScrollPane = new JScrollPane(Jtable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setBounds(10, 80, 780, 300);
        add(tableScrollPane);

        processingTimeLBL = new JLabel("Processing Time: ");
        processingTimeLBL.setBounds(15, 375, 150, 30);
        add(processingTimeLBL);

        processingTime = new JLabel("0.0 milliseconds");
        processingTime.setBounds(140, 375, 150, 30);
        add(processingTime);

        setTitle("Enter your search query");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 100);
        setResizable(false);
        setLayout(null);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Check query syntax, process query if query is in proper format
     */
    private boolean startQueryProcessor(boolean showErrors, long sTime) {
        String query = queryTF.getText().trim();
        if (showErrors && queryHistory.containsKey(query)) {
            queryResult = queryHistory.get(query);
            showResultPanel();
            processingTime.setText(BigDecimal.valueOf(((double) System.nanoTime() - sTime) / 1000000)
                    + " milliseconds");
            System.out.println("From Cache mem");
            return true;
        }
        boolean ret = false;
        // check if query syntax is not valid
        if (query.length() > 0 && syntaxChecker.isValidQuery(query)) {
            if (showErrors && query.equals("EXIT")) {
                // 0 -> yes, 1 -> no, -1 -> dialog closed
                int n = JOptionPane.showOptionDialog(this,
                        "Would you like quit search application?",
                        "Exit Application",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null);
                if (n == 0) {
                    this.dispose();
                }
                queryTF.setText("");
                return false;
            }
            Set<Integer> processQuery = queryProcessor.processQuery(query);
            queryResult = new ArrayList<>(processQuery);
            if (!queryResult.isEmpty()) {
                tableModel.fireTableDataChanged();
                showResultPanel();
                if (showErrors) {
                    processingTime.setText(BigDecimal.valueOf(((double) System.nanoTime() - sTime) / 1000000)
                            + " milliseconds");
                }
            } else {
                hideResultPanel();
                if (showErrors) {
                    JOptionPane.showMessageDialog(this,
                            "No documents satisfies that query..!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
            ret = true;
        }
        return ret;
    }

    public final void showResultPanel() {
        this.setSize(800, 425);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
    }

    public final void hideResultPanel() {
        this.setSize(800, 100);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(""));
        chooser.setDialogTitle("Select any directory with text files");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): " + chooser.getCurrentDirectory());
            System.out.println("getSelectedFile() : " + chooser.getSelectedFile());
        } else {
            System.out.println("No Selection ");
        }
    }

    class TableModel extends AbstractTableModel {

        private final String[] columnNames = {"File Name"};
        private final Class[] columnClass = new Class[]{String.class};

        public TableModel() {
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column]; //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClass[columnIndex];
        }

        @Override
        public int getRowCount() {
            return queryResult.size();
        }

        @Override
        public int getColumnCount() {
            return columnClass.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return fileNames.get(queryResult.get(rowIndex));
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private class KeyListenerImpl implements KeyListener {

        public KeyListenerImpl() {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_ENTER) {
                if (startQueryProcessor(true, System.nanoTime())) {
                    queryHistory.put(((JTextField) e.getSource()).getText().trim(),
                            queryResult);
//                    queryHistoryPointer = queryHistory.size();
                }
            } else if (key == KeyEvent.VK_ESCAPE) {
                ((JTextField) e.getSource()).setText("");
                hideResultPanel();
            }
//            else if (key == KeyEvent.VK_UP) {
//                if (queryHistoryPointer > 0) {
//                    queryHistoryPointer--;
//                    ((JTextField) e.getSource()).setText(queryHistory.get(queryHistoryPointer));
//                }
//            } else if (key == KeyEvent.VK_DOWN) {
//                if (queryHistoryPointer < queryHistory.size() - 1) {
//                    queryHistoryPointer++;
//                    ((JTextField) e.getSource()).setText(queryHistory.get(queryHistoryPointer));
//                }
//            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            String text = ((JTextField) e.getSource()).getText();
            if (key == KeyEvent.VK_DELETE || key == KeyEvent.VK_BACK_SPACE) {
                if (text.equals("")) {
                    hideResultPanel();
                } else if (text.length() >= 3) {
                    startQueryProcessor(false, System.nanoTime());
                }
            } else if (key != KeyEvent.VK_ENTER && key != KeyEvent.VK_ESCAPE) {
                if (text.length() >= 3) {
                    startQueryProcessor(false, System.nanoTime());
                } else if (text.length() < 3) {
                    hideResultPanel();
                }
            }
        }
    }

    private class ActionListenerImpl implements ActionListener {

        public ActionListenerImpl() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (startQueryProcessor(true, System.nanoTime())) {
                queryHistory.put(queryTF.getText().trim(), queryResult);
//                queryHistoryPointer = queryHistory.size();
            }
        }
    }
}
