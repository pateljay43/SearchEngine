/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author JAY
 */
public class GUI extends JFrame implements MouseListener {

    private final DecimalFormat df2;
    private final PositionalInvertedIndex index;
    private final QueryProcessor queryProcessor;
    private final QuerySyntaxCheck syntaxChecker;
    private final ArrayList<String> fileNames;
    private static HashMap<String, List<Integer>> queryHistory;
    private static ArrayList<String> queryHistoryArray;
    private static int queryHistoryPointer;
    private List<Integer> queryResult;

    private final JTextField queryTF;
    private final JButton searchBtn;
    private final JButton newDirectoryBtn;
    // result table
    private final TableModel tableModel;
    private final JTable Jtable;
    private final JScrollPane tableScrollPane;
    private final JLabel processingTimeLBL;
    private final JLabel processingTime;
    private final JLabel numOfDocumentsLBL;
    private final JLabel numOfDocuments;
    private final JLabel indexStatisticsLBL;
    private boolean changeIndex;
    private final Path currentWorkingPath;
    private final int numOfTypes;
    private boolean quit;
    private final int offset;

    public GUI(PositionalInvertedIndex _index, ArrayList<String> _fileNames,
            Path _currentWorkingPath, int _numOfTypes) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            offset = 30;
        } else {
            offset = 20;
        }
        quit = false;
        numOfTypes = _numOfTypes;
        currentWorkingPath = _currentWorkingPath;
        changeIndex = false;
        index = _index;
        queryProcessor = new QueryProcessor(_index);
        fileNames = _fileNames;
        syntaxChecker = new QuerySyntaxCheck();
        queryResult = new ArrayList<>();
        queryHistory = new HashMap<>();
        queryHistoryArray = new ArrayList<>();
        queryHistoryPointer = 0;
        df2 = new DecimalFormat("#.##");

        indexStatisticsLBL = new JLabel("Index Statistics:-  Press (Ctrl + i)");
        indexStatisticsLBL.setBounds(15, 5, 250, 30);
        add(indexStatisticsLBL);

        newDirectoryBtn = new JButton("Change Folder");
        newDirectoryBtn.setBounds(670, 7, 120, 25);
        newDirectoryBtn.addActionListener((ActionEvent e) -> {
            setChangeIndex();
        });
        newDirectoryBtn.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK), "ctrl+i");
        newDirectoryBtn.getActionMap().put("ctrl+i", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showStatistics();
            }
        });
        add(newDirectoryBtn);

        queryTF = new JTextField();
        queryTF.setBounds(10, 40, 650, 25);
        queryTF.addKeyListener(new KeyListenerImpl());
        queryTF.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK), "ctrl+i");
        queryTF.getActionMap().put("ctrl+i", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showStatistics();
            }
        });
        add(queryTF);

        searchBtn = new JButton("Search");
        searchBtn.setBounds(670, 40, 120, 25);
        searchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (startQueryProcessor(true, System.nanoTime())) {
                    queryHistoryPointer = queryHistory.size() - 1;
                }
            }
        });
        searchBtn.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK), "ctrl+i");
        searchBtn.getActionMap().put("ctrl+i", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showStatistics();
            }
        });
        add(searchBtn);

        tableModel = new TableModel();
        Jtable = new JTable(tableModel);
        Jtable.setGridColor(Color.gray);
        Jtable.setShowVerticalLines(false);
        Jtable.addMouseListener(this);
        Jtable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK), "ctrl+i");
        Jtable.getActionMap().put("ctrl+i", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showStatistics();
            }
        });
        Jtable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        Jtable.getActionMap().put("enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = ((JTable) e.getSource()).getSelectedRow();
                openFile(row);
            }
        });
        tableScrollPane = new JScrollPane(Jtable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setBounds(10, 75, 780, 300);
        add(tableScrollPane);

        processingTimeLBL = new JLabel("Processing Time: ");
        processingTimeLBL.setBounds(15, 375, 150, 30);
        add(processingTimeLBL);

        processingTime = new JLabel("0.0 milliseconds");
        processingTime.setBounds(140, 375, 150, 30);
        add(processingTime);

        numOfDocumentsLBL = new JLabel("Number Of Documents: ");
        numOfDocumentsLBL.setBounds(450, 375, 150, 30);
        add(numOfDocumentsLBL);

        numOfDocuments = new JLabel("0");
        numOfDocuments.setBounds(610, 375, 100, 30);
        add(numOfDocuments);

        setTitle("Enter your search query");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 70 + offset);
        setResizable(false);
        setLayout(null);
        setLocationRelativeTo(null);
        setVisible(true);

        queryTF.requestFocus();
    }

    /**
     * Check query syntax, process query if query is in proper format
     */
    private boolean startQueryProcessor(boolean showErrors, long sTime) {
        String query = queryTF.getText().trim();
        if (showErrors && queryHistory.containsKey(query)) {
            queryResult = queryHistory.get(query);
            addToHistory(query);
            showResultPanel(showErrors);
            processingTime.setText(BigDecimal.valueOf(((double) System.nanoTime() - sTime) / 1000000)
                    + " milliseconds");
            numOfDocuments.setText(queryResult.size() + "");
            System.out.println("From Cache mem");
            return true;
        }
        boolean ret = false;
        // check if query syntax is not valid
        if (!syntaxChecker.isValidQuery(query)) {       // invalid query
            hideResultPanel();
            if (showErrors) {
                JOptionPane.showMessageDialog(this,
                        syntaxChecker.getErrorMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {        // valid query
            if (showErrors && query.equals("EXIT")) {
                // 0 -> yes, 1 -> no, -1 -> dialog closed
                int n = JOptionPane.showOptionDialog(this,
                        "Would you like quit search application?",
                        "Exit Application",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null);
                if (n == 0) {
                    changeIndex = true;
                    quit = true;
                    this.dispose();
                }
                queryTF.setText("");
                return false;
            }
            queryResult = new ArrayList<>(queryProcessor.processQuery(query));
            if (!queryResult.isEmpty()) {
                tableModel.fireTableDataChanged();
                showResultPanel(showErrors);
                if (showErrors) {
                    processingTime.setText(BigDecimal.valueOf(((double) System.nanoTime() - sTime) / 1000000)
                            + " milliseconds");
                    numOfDocuments.setText(queryResult.size() + "");
                    addToHistory(query);
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

    public final void showResultPanel(boolean show) {
        if (!show && queryHistory.containsKey(queryTF.getText())) {
            show = true;
        }
        processingTime.setVisible(show);
        processingTimeLBL.setVisible(show);
        numOfDocuments.setVisible(show);
        numOfDocumentsLBL.setVisible(show);
        setSize(800, 405 + offset);
        setLocationRelativeTo(null);
    }

    public final void hideResultPanel() {
        if (queryHistory.containsKey(queryTF.getText())) {
            return;
        }
        setSize(800, 70 + offset);
        setLocationRelativeTo(null);
    }

    private void setChangeIndex() {
        this.setVisible(false);
        this.changeIndex = true;
    }

    private void openFile(int row) {
        Desktop desktop = Desktop.getDesktop();
        String fileURI = currentWorkingPath + "/" + Jtable.getValueAt(row, 0);
        try {
            desktop.open(new File(fileURI));
        } catch (IOException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean isChangeIndex() {
        return changeIndex;
    }

    public boolean isQuit() {
        return quit;
    }

    private void addToHistory(String query) {
        if (queryHistory.containsKey(query)) {
            queryHistoryArray.add(queryHistoryArray.remove(queryHistoryArray.indexOf(query)));
        } else {
            queryHistory.put(query, queryResult);
            queryHistoryArray.add(query);
            queryHistoryPointer = queryHistory.size() - 1;
        }
    }

    /**
     * # of Terms, # of Types, Average number of documents per term, 10 most
     * frequent terms, Approximate total memory required by index
     */
    private void showStatistics() {
        JOptionPane.showMessageDialog(this,
                "Number of Terms: " + index.getTermCount() + "\n"
                + "Number of Types (distinct tokens): " + numOfTypes + "\n"
                + "Average number of documents per term: " + df2.format(index.getAvgDocPerTerm()) + "\n"
                + "Approximate total memory requirement: "
                + df2.format(index.getTotalMemory() / Math.pow(2.00, 20))
                + " Megabytes\n"
                + "10 most frequent words statistics: (term, document frequency) \n"
                + index.getMostFreqTerms(),
                "Index Statistics", JOptionPane.INFORMATION_MESSAGE
        );
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
                    queryHistoryPointer = queryHistory.size() - 1;
                }
            } else if (key == KeyEvent.VK_ESCAPE) {
                ((JTextField) e.getSource()).setText("");
                queryHistoryPointer = queryHistory.size();
                hideResultPanel();
            } else if (key == KeyEvent.VK_UP) {
                if (queryHistoryPointer > 0) {
                    long sTime = System.nanoTime();
                    queryHistoryPointer--;
                    String query = queryHistoryArray.get(queryHistoryPointer);
                    ((JTextField) e.getSource()).setText(query);
                    queryResult = queryHistory.get(query);
                    if (!queryResult.isEmpty()) {
                        showResultPanel(true);
                        processingTime.setText(BigDecimal
                                .valueOf(((double) System.nanoTime() - sTime) / 1000000)
                                + " milliseconds");
                        numOfDocuments.setText(queryResult.size() + "");
                    }
                }
            } else if (key == KeyEvent.VK_DOWN) {
                if (queryHistoryPointer < queryHistory.size() - 1) {
                    long sTime = System.nanoTime();
                    queryHistoryPointer++;
                    String query = queryHistoryArray.get(queryHistoryPointer);
                    ((JTextField) e.getSource()).setText(query);
                    queryResult = queryHistory.get(query);
                    if (!queryResult.isEmpty()) {
                        showResultPanel(true);
                        processingTime.setText(BigDecimal
                                .valueOf(((double) System.nanoTime() - sTime) / 1000000)
                                + " milliseconds");
                        numOfDocuments.setText(queryResult.size() + "");
                    }
                }
            }
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

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            int row = ((JTable) e.getSource()).getSelectedRow();
            openFile(row);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
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
}
