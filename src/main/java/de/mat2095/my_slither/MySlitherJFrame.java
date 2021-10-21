package de.mat2095.my_slither;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;


//built on JFrame used for making windows.
final class MySlitherJFrame extends JFrame {

    //what is going to be all the available skins. Not yet implemented.
    private static final String[] SNAKES = {
        "00 - purple",
        "01 - blue",
        "02 - cyan",
        "03 - green",
        "04 - yellow",
        "05 - orange",
        "06 - salmon",
        "07 - red",
        "08 - violet",
        "09 - flag: USA",
        "10 - flag: Russia",
        "11 - flag: Germany",
        "12 - flag: Italy",
        "13 - flag: France",
        "14 - white/red",
        "15 - rainbow",
        "16 - blue/yellow",
        "17 - white/blue",
        "18 - red/white",
        "19 - white",
        "20 - green/purple",
        "21 - flag: Brazil",
        "22 - flag: Ireland",
        "23 - flag: Romania",
        "24 - cyan/yellow +extra",
        "25 - purple/orange +extra",
        "26 - grey/brown",
        "27 - green with eye",
        "28 - yellow/green/red",
        "29 - black/yellow",
        "30 - stars/EU",
        "31 - stars",
        "32 - EU",
        "33 - yellow/black",
        "34 - colorful",
        "35 - red/white/pink",
        "36 - blue/white/light-blue",
        "37 - Kwebbelkop",
        "38 - yellow",
        "39 - PewDiePie",
        "40 - green happy",
        "41 - red with eyes",
        "42 - Google Play",
        "43 - UK",
        "44 - Ghost",
        "45 - Canada",
        "46 - Swiss",
        "47 - Moldova",
        "48 - Vietnam",
        "49 - Argentina",
        "50 - Colombia",
        "51 - Thailand",
        "52 - red/yellow",
        "53 - glowy-blue",
        "54 - glowy-red",
        "55 - glowy-yellow",
        "56 - glowy-orange",
        "57 - glowy-purple",
        "58 - glowy-green",
        "59 - yellow-M",
        "60 - detailed UK",
        "61 - glowy-colorful",
        "62 - purple spiral",
        "63 - red/black",
        "64 - blue/black"
    };

    // TODO: skins, prey-size, snake-length/width, bot-layer, that-other-thing(?), show ping

    private final JTextField server, name;
    private final JComboBox<String> snake;
    private final JCheckBox useRandomServer;
    private final JToggleButton connect;
    private final JLabel rank, kills;
    private final JSplitPane rightSplitPane, fullSplitPane;
    private final JTextArea log;
    private final JScrollBar logScrollBar;
    private final JTable highscoreList;
    private final MySlitherCanvas canvas; //game canvas. extended from  JPanel.

    private final long startTime;
    private final Timer updateTimer;
    private Status status;
    private URI[] serverList;
    private MySlitherWebSocketClient client;
    private final Player player;
    MySlitherModel model;
    final Object modelLock = new Object();

    MySlitherJFrame() {
        super("MySlither"); //calls the parent classes constructor. Passes it the window title
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); //JFrame function tells it to actually close on close.
        addWindowListener(new WindowAdapter() { //adds a window listener. used to monitor user input.
            //configuration for WindowAdapter. Think its some sort of interface for a listener. Not entirely certain though and don't think I need to dig deeper.
            @Override
            public void windowClosing(WindowEvent e) {
                updateTimer.cancel();
                if (status == Status.CONNECTING || status == Status.CONNECTED) {
                    disconnect();
                }
                canvas.repaintThread.shutdown();
                try {
                    canvas.repaintThread.awaitTermination(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        //This is the top level pane where content is placed. And then sets its layout.
        getContentPane().setLayout(new BorderLayout());

        //creates a new instance. Pretty sure this is where gameplay is going to be.
        canvas = new MySlitherCanvas(this); //passes in the JFrame. think this is used to control synchronisation for drawing snakes but not sure.
        player = canvas.mouseInput;         //something to do with getting user input.

        // === upper row ===
        JPanel settings = new JPanel(new GridBagLayout());//creates a new panel to place the settings on.

        //declares a bunch of elements used in the settings.
        server = new JTextField(18);

        //name input.
        name = new JTextField("MySlitherEaterBot", 16);

        //colour option.
        snake = new JComboBox<>(SNAKES);
        snake.setMaximumRowCount(snake.getItemCount());

        useRandomServer = new JCheckBox("use random server", true);
        useRandomServer.addActionListener(a -> {
            setStatus(null);
        });

        //configures connect button.
        connect = new JToggleButton();
        connect.addActionListener(a -> {
            switch (status) {       //used to basically start the game. if game is already running disconnect. otherwise connect.
                case DISCONNECTED:
                    connect();
                    break;
                case CONNECTING:
                case CONNECTED:
                    disconnect();
                    break;
                case DISCONNECTING:
                    break;
            }
        });
        //implements a listener for connect in all classes it may be derived from.
        connect.addAncestorListener(new AncestorListener() {
            //runs when the button is changed from visible to invisible. Pretty sure this only happens once.
            @Override
            public void ancestorAdded(AncestorEvent event) {
                connect.requestFocusInWindow();
                connect.removeAncestorListener(this);
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });

        rank = new JLabel();

        kills = new JLabel();

        settings.add(new JLabel("server:"),
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(server,
            new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("name:"),
            new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(name,
            new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("skin:"),
            new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(snake,
            new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(useRandomServer,
            new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(connect,
            new GridBagConstraints(2, 1, 1, 2, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JSeparator(SwingConstants.VERTICAL),
            new GridBagConstraints(3, 0, 1, 3, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 6, 0, 6), 0, 0));
        settings.add(new JLabel("kills:"),
            new GridBagConstraints(4, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(kills,
            new GridBagConstraints(5, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("rank:"),
            new GridBagConstraints(4, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(rank,
            new GridBagConstraints(5, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));

        JComponent upperRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperRow.add(settings);
        getContentPane().add(upperRow, BorderLayout.NORTH);

        // === center === //centre left
        log = new JTextArea("hi");
        log.setEditable(false);
        log.setLineWrap(true);
        log.setFont(Font.decode("Monospaced 11"));
        log.setTabSize(4);
        log.getCaret().setSelectionVisible(false);
        log.getInputMap().clear();
        log.getActionMap().clear();
        log.getInputMap().put(KeyStroke.getKeyStroke("END"), "gotoEnd");
        log.getActionMap().put("gotoEnd", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> {
                    logScrollBar.setValue(logScrollBar.getMaximum() - logScrollBar.getVisibleAmount());
                });
            }
        });
        log.getInputMap().put(KeyStroke.getKeyStroke("HOME"), "gotoStart");
        log.getActionMap().put("gotoStart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> {
                    logScrollBar.setValue(logScrollBar.getMinimum());
                });
            }
        });

        //centre right
        highscoreList = new JTable(10, 2);
        highscoreList.setEnabled(false);
        highscoreList.getColumnModel().getColumn(0).setMinWidth(64);
        highscoreList.getColumnModel().getColumn(1).setMinWidth(192);
        highscoreList.getColumnModel().getColumn(0).setHeaderValue("length");
        highscoreList.getColumnModel().getColumn(1).setHeaderValue("name");
        highscoreList.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        highscoreList.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
        highscoreList.setPreferredScrollableViewportSize(new Dimension(64 + 192, highscoreList.getPreferredSize().height));

        // == split-panes ==
        rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, canvas, new JScrollPane(highscoreList));
        rightSplitPane.setDividerSize(rightSplitPane.getDividerSize() * 4 / 3);
        rightSplitPane.setResizeWeight(0.99);

        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setPreferredSize(new Dimension(300, logScrollPane.getPreferredSize().height));
        logScrollBar = logScrollPane.getVerticalScrollBar();
        fullSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, logScrollPane, rightSplitPane);
        fullSplitPane.setDividerSize(fullSplitPane.getDividerSize() * 4 / 3);
        fullSplitPane.setResizeWeight(0.1);

        getContentPane().add(fullSplitPane, BorderLayout.CENTER);

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        setSize(screenWidth * 3 / 4, screenHeight * 4 / 5);
        setLocation((screenWidth - getWidth()) / 2, (screenHeight - getHeight()) / 2);
        setExtendedState(MAXIMIZED_BOTH);

        validate(); //tells the JFrame that its width height and location have all been set.
        startTime = System.currentTimeMillis(); //stores the current time. Used in logging.
        setStatus(Status.DISCONNECTED);         //sets the initial state. Not connected to the game server.

        //sets up the timer. I'm pretty sure this is the gameplay timer but no clue what it does.
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() { //the function updateTimer calls to update.
                synchronized (modelLock) {
                    if (status == Status.CONNECTED && model != null) {
                        model.update();
                        client.sendData(player.action(model));
                    }
                }
            }
        }, 1, 10); //do not replace this value. If anything makes it worse.
    }

    //Is called from websocket. Think its being called when a socket/connection is opened.
    void onOpen() {
        switch (status) {
            case CONNECTING:    //connection made while connecting.
                setStatus(Status.CONNECTED);    //update status. Now know we've connected.
                client.sendInitRequest(snake.getSelectedIndex(), name.getText());   //passes off name and colour to the slither server.
                break;
            case DISCONNECTING: //connection made while disconnecting. Not sure why this would happened but just disconnects anyway
                disconnect();
                break;
            default:
                throw new IllegalStateException("Connected while not connecting!");
        }
    }

    //called on closing a connection. Mostly just sets settings.
    void onClose() {
        switch (status) {
            case CONNECTED:
            case DISCONNECTING:
                setStatus(Status.DISCONNECTED);
                client = null;
                break;
            case CONNECTING:    //think this means connection failed / full or something and try a new connection. Makes a new one at least.
                client = null;
                trySingleConnect();
                break;
            default:
                throw new IllegalStateException("Disconnected while not connecting, connected or disconnecting!");
        }
    }

    private void connect() {
        new Thread(() -> { //creates a new thread and then bit below is function it runs.
            if (status != Status.DISCONNECTED) {
                throw new IllegalStateException("Connecting while not disconnected");
            }
            setStatus(Status.CONNECTING);

            MySlitherCanvas.setSnakeColour((String) snake.getSelectedItem());
            setModel(null); //still no clue what model is but resets it.

            //gets a list of servers from slither and checks it actually got some.
            if (useRandomServer.isSelected()) {
                log("fetching server-list...");
                serverList = MySlitherWebSocketClient.getServerList();
                log("received " + serverList.length + " servers");
                if (serverList.length <= 0) {
                    log("no server found");
                    setStatus(Status.DISCONNECTED);
                    return;
                }
            }

            //attempts to initiate a connection.
            if (status == Status.CONNECTING) {
                trySingleConnect();
            }
        }).start();
    }

    private void trySingleConnect() {
        if (status != Status.CONNECTING) {
            throw new IllegalStateException("Trying single connection while not connecting");
        }

        //connects to a server.
        if (useRandomServer.isSelected()) {
            //makes/defines connection. But does not connect.
            client = new MySlitherWebSocketClient(serverList[(int) (Math.random() * serverList.length)], this);
            server.setText(client.getURI().toString());     //updates server text box. Basically letting user know what server they're on.
        } else {
            //same process but this time its taking the server from the server text box and has to check its valid.
            try {
                client = new MySlitherWebSocketClient(new URI(server.getText()), this);
            } catch (URISyntaxException ex) {
                log("invalid server");
                setStatus(Status.DISCONNECTED);
                return;
            }
        }

        //initiate connection.
        log("connecting to " + client.getURI() + " ...");
        client.connect();
    }

    //pretty obvious
    private void disconnect() {
        if (status == Status.DISCONNECTED) {
            throw new IllegalStateException("Already disconnected");
        }
        setStatus(Status.DISCONNECTING);
        if (client != null) {
            client.close();
        }
    }

    //updates the status every where its used.
    private void setStatus(Status newStatus) {
        if (newStatus != null) {
            status = newStatus;
        }
        connect.setText(status.buttonText);
        connect.setSelected(status.buttonSelected);
        connect.setEnabled(status.buttonEnabled);
        server.setEnabled(status.allowModifyData && !useRandomServer.isSelected());
        useRandomServer.setEnabled(status.allowModifyData);
        name.setEnabled(status.allowModifyData);
        snake.setEnabled(status.allowModifyData);
    }

    //used to print logging text.
    void log(String text) {
        print(String.format("%6d\t%s", System.currentTimeMillis() - startTime, text));
    }

    //also used prints to the lgging window.
    private void print(String text) {
        SwingUtilities.invokeLater(() -> {
            boolean scrollToBottom = !logScrollBar.getValueIsAdjusting() && logScrollBar.getValue() >= logScrollBar.getMaximum() - logScrollBar.getVisibleAmount();
            log.append('\n' + text);
            fullSplitPane.getLeftComponent().validate();
            if (scrollToBottom) {
                logScrollBar.setValue(logScrollBar.getMaximum() - logScrollBar.getVisibleAmount());
            }
        });
    }

    //resets stats and sets the model to whatever is passed.
    void setModel(MySlitherModel model) {
        synchronized (modelLock) {
            this.model = model;
            rank.setText(null);
            kills.setText(null);
        }
    }

    //passes the map to the canvas but I have no idea how its being formatted or used.
    void setMap(boolean[] map) {
        canvas.setMap(map);
    }

    //updates the rank stat
    void setRank(int newRank, int playerCount) {
        rank.setText(newRank + "/" + playerCount);
    }

    //updates the kills stat.
    void setKills(int newKills) {
        kills.setText(String.valueOf(newKills));
    }

    //used to place the values passed into the highscore table.
    void setHighscoreData(int row, String name, int length, boolean highlighted) {
        highscoreList.setValueAt(highlighted ? "<html><b>" + length + "</b></html>" : length, row, 0);
        highscoreList.setValueAt(highlighted ? "<html><b>" + name + "</b></html>" : name, row, 1);
    }

    //connection status.
    private enum Status {
        //implementations.
        DISCONNECTED("connect", false, true, true),
        CONNECTING("connecting...", true, true, false),
        CONNECTED("disconnect", true, true, false),
        DISCONNECTING("disconnecting...", false, false, false);

        //used to define different aspects of the status.
        private final String buttonText;
        private final boolean buttonSelected, buttonEnabled;
        private final boolean allowModifyData;

        //constructor for building statusi
        private Status(String buttonText, boolean buttonSelected, boolean buttonEnabled, boolean allowModifyData) {
            this.buttonText = buttonText;
            this.buttonSelected = buttonSelected;
            this.buttonEnabled = buttonEnabled;
            this.allowModifyData = allowModifyData;
        }
    }
}
