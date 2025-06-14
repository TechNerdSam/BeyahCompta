import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BeyahCompta extends JFrame {

    private List<Transaction> transactions;
    private Map<String, Double> accountBalances;

    private JTable transactionsTable;
    private DefaultTableModel tableModel;
    private JLabel soldeGlobalLabel;
    private JComboBox<String> filterTypeComboBox;
    private JComboBox<String> filterCategoryComboBox;
    private JTextField searchField;

    // Rapports
    private JLabel totalDebitLabel;
    private JLabel totalCreditLabel;
    private JPanel accountBalancesPanel;

    // Champs pour la saisie de transaction - Déclarés comme membres de la classe pour un accès facile
    private JTextField descriptionField;
    private JTextField montantField;
    private JComboBox<String> typeComboBox;
    private JComboBox<String> categoryComboBox;
    private JComboBox<String> accountComboBox;

    // Fichiers de persistance
    private final String DATA_DIR_PATH;
    private final String TRANSACTIONS_FILE;
    private final String BALANCES_FILE;

    // Couleurs conviviales, lifestyle et stylées (inspirées du Material Design pour l'harmonie)
    private final Color PRIMARY_COLOR = new Color(3, 169, 244); // Bleu clair vif (Light Blue 500)
    private final Color SECONDARY_COLOR = new Color(21, 101, 192); // Bleu foncé (Blue 800)
    private final Color ACCENT_COLOR_POSITIVE = new Color(76, 175, 80); // Vert frais (Green 500)
    private final Color ACCENT_COLOR_NEGATIVE = new Color(244, 67, 54); // Rouge adaptable (Red 500)
    private final Color BACKGROUND_COLOR = new Color(236, 239, 241); // Gris très clair pour le fond (Blue Grey 50)
    private final Color PANEL_BACKGROUND_COLOR = Color.WHITE; // Blanc pur pour les panneaux
    private final Color TEXT_COLOR_DARK = new Color(33, 33, 33); // Gris très foncé pour le texte principal (Grey 900)
    // private final Color TEXT_COLOR_LIGHT = new Color(158, 158, 158); // Gris moyen pour le texte secondaire (Grey 500) - Supprimé car non utilisé

    // Utilisation de polices sans-serif system-friendly
    private final String FONT_NAME = "Segoe UI"; // Bonne police sur Windows, sinon Fallback
    private final Font APP_TITLE_HEADER_FONT = new Font("Script MT Bold", Font.BOLD, 36);
    private final Font HEADER_FONT = new Font(FONT_NAME, Font.BOLD, 32);
    private final Font SUBHEADER_FONT = new Font(FONT_NAME, Font.BOLD, 20);
    private final Font LABEL_FONT = new Font(FONT_NAME, Font.BOLD, 15);
    private final Font DATA_FONT = new Font(FONT_NAME, Font.PLAIN, 14);
    private final Font MONETARY_FONT = new Font(FONT_NAME, Font.BOLD, 16);

    public BeyahCompta() {
        // Initialisation des chemins de fichiers pour la persistance
        String userHome = System.getProperty("user.home");
        DATA_DIR_PATH = userHome + File.separator + "BeyahComptaData";
        TRANSACTIONS_FILE = DATA_DIR_PATH + File.separator + "transactions.csv";
        BALANCES_FILE = DATA_DIR_PATH + File.separator + "account_balances.txt";

        // Créer le répertoire de données s'il n'existe pas
        new File(DATA_DIR_PATH).mkdirs();

        transactions = new ArrayList<>();
        accountBalances = new HashMap<>();

        // Initialiser les soldes des comptes par défaut si aucun fichier de balance n'existe
        // Ou charger les soldes existants
        accountBalances.put("Caisse", 0.0);
        accountBalances.put("Banque", 0.0);
        accountBalances.put("Épargne", 0.0);

        // Charger les données persistantes au démarrage
        loadData();

        setTitle("BéyahCompta - Gestion Financière Épurée");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(20, 20)); // Marges généreuses pour l'ensemble

        // --- Appliquer le Look and Feel Nimbus ---
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize Nimbus Look and Feel. Falling back to default L&F.");
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                // Ignore
            }
        }
        // Ajustements visuels pour le L&F Nimbus avec les nouvelles couleurs
        UIManager.put("control", BACKGROUND_COLOR); // Couleur de fond des contrôles
        UIManager.put("info", PANEL_BACKGROUND_COLOR); // Couleur d'information (tooltips, etc.)
        UIManager.put("nimbusBase", PRIMARY_COLOR); // Couleur de base pour les éléments clés de Nimbus
        UIManager.put("nimbusBlueGrey", SECONDARY_COLOR); // Couleur secondaire pour les bordures/accents
        UIManager.put("text", TEXT_COLOR_DARK); // Couleur par défaut du texte

        // --- En-tête Global ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(PRIMARY_COLOR);
        topPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        add(topPanel, BorderLayout.NORTH);

        JLabel appTitleLabel = new JLabel("BéyahCompta");
        appTitleLabel.setFont(APP_TITLE_HEADER_FONT);
        appTitleLabel.setForeground(Color.WHITE);
        topPanel.add(appTitleLabel, BorderLayout.WEST);

        soldeGlobalLabel = new JLabel("Solde Global: " + formatCurrency(getGlobalBalance()));
        soldeGlobalLabel.setFont(HEADER_FONT.deriveFont(Font.BOLD, 30));
        soldeGlobalLabel.setForeground(Color.WHITE);
        soldeGlobalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(soldeGlobalLabel, BorderLayout.EAST);

        // --- Contenu Central avec Onglets ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(LABEL_FONT);
        tabbedPane.setBackground(BACKGROUND_COLOR);
        tabbedPane.setForeground(TEXT_COLOR_DARK);
        // Personnalisation des onglets pour Nimbus (peut varier légèrement)
        UIManager.put("TabbedPane.contentAreaColor", BACKGROUND_COLOR);
        UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(0,0,0,0));
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 15, 8, 15));
        UIManager.put("TabbedPane.selectedBackground", PRIMARY_COLOR);
        UIManager.put("TabbedPane.unselectedBackground", SECONDARY_COLOR.brighter()); // Ajusté pour le nouveau SECONDARY_COLOR
        UIManager.put("TabbedPane.unselectedForeground", TEXT_COLOR_DARK);
        UIManager.put("TabbedPane.selected", PRIMARY_COLOR);


        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addTab("Transactions", createTransactionsPanel());
        
        JPanel reportsPanel = new JPanel(new BorderLayout(20, 20));
        reportsPanel.setBackground(BACKGROUND_COLOR);
        reportsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        tabbedPane.addTab("Rapports", createReportsPanel());

        updateUI(); // Initialiser l'affichage

        // Sauvegarder les données à la fermeture de l'application
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
            }
        });
    }

    // Helper method to apply hover effects to buttons
    private void applyHoverEffect(JButton button, Color normalColor, Color hoverColor) {
        button.setBackground(normalColor);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(normalColor);
            }
        });
    }

    private JPanel createTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_COLOR);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBackground(BACKGROUND_COLOR);
        splitPane.setBorder(BorderFactory.createEmptyBorder()); // Supprimer la bordure par défaut du splitPane
        panel.add(splitPane, BorderLayout.CENTER);

        // Panel de saisie de transaction
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(PANEL_BACKGROUND_COLOR);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(25, 25, 25, 25), // Padding interne
                BorderFactory.createLineBorder(new Color(220, 220, 220)) // Bordure simple pour le panneau
        ));
        splitPane.setLeftComponent(inputPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel inputHeader = new JLabel("Nouvelle Transaction");
        inputHeader.setFont(SUBHEADER_FONT);
        inputHeader.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        inputPanel.add(inputHeader, gbc);

        gbc.gridwidth = 1;

        // Compte
        gbc.gridy++;
        JLabel accountLabel = new JLabel("Compte:");
        accountLabel.setFont(LABEL_FONT);
        accountLabel.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0;
        inputPanel.add(accountLabel, gbc);
        accountComboBox = new JComboBox<>(accountBalances.keySet().toArray(new String[0]));
        accountComboBox.setFont(DATA_FONT);
        accountComboBox.setBackground(Color.WHITE);
        accountComboBox.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        inputPanel.add(accountComboBox, gbc);

        // Type de transaction
        gbc.gridy++;
        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setFont(LABEL_FONT);
        typeLabel.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0;
        inputPanel.add(typeLabel, gbc);
        typeComboBox = new JComboBox<>(new String[]{"Débit", "Crédit"});
        typeComboBox.setFont(DATA_FONT);
        typeComboBox.setBackground(Color.WHITE);
        typeComboBox.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        inputPanel.add(typeComboBox, gbc);

        // Catégorie
        gbc.gridy++;
        JLabel categoryLabel = new JLabel("Catégorie:");
        categoryLabel.setFont(LABEL_FONT);
        categoryLabel.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0;
        inputPanel.add(categoryLabel, gbc);
        categoryComboBox = new JComboBox<>(new String[]{"Général", "Nourriture", "Transport", "Loisirs", "Salaire", "Autre"});
        categoryComboBox.setFont(DATA_FONT);
        categoryComboBox.setBackground(Color.WHITE);
        categoryComboBox.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        inputPanel.add(categoryComboBox, gbc);

        // Description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Description:");
        descriptionLabel.setFont(LABEL_FONT);
        descriptionLabel.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0;
        inputPanel.add(descriptionLabel, gbc);
        descriptionField = new JTextField(20);
        descriptionField.setFont(DATA_FONT);
        descriptionField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        inputPanel.add(descriptionField, gbc);

        // Montant
        gbc.gridy++;
        JLabel montantLabel = new JLabel("Montant:");
        montantLabel.setFont(LABEL_FONT);
        montantLabel.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0;
        inputPanel.add(montantLabel, gbc);
        montantField = new JTextField(10);
        montantField.setFont(DATA_FONT);
        montantField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        inputPanel.add(montantField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton addTransactionButton = new JButton("Ajouter Transaction");
        addTransactionButton.setFont(LABEL_FONT.deriveFont(Font.BOLD, 16));
        addTransactionButton.setForeground(Color.WHITE);
        addTransactionButton.setFocusPainted(false);
        addTransactionButton.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        addTransactionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyHoverEffect(addTransactionButton, PRIMARY_COLOR, PRIMARY_COLOR.brighter());
        inputPanel.add(addTransactionButton, gbc);

        // Panel d'affichage et de filtrage des transactions
        JPanel displayPanel = new JPanel(new BorderLayout(15, 15));
        displayPanel.setBackground(BACKGROUND_COLOR);
        splitPane.setRightComponent(displayPanel);

        // Panel de filtrage et recherche
        JPanel topTablePanel = new JPanel(new BorderLayout(15, 15));
        topTablePanel.setBackground(PANEL_BACKGROUND_COLOR);
        topTablePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(new Color(220, 220, 220))
        ));
        displayPanel.add(topTablePanel, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        filterPanel.setBackground(PANEL_BACKGROUND_COLOR);
        JLabel filterLabel = new JLabel("Filtrer par:");
        filterLabel.setFont(LABEL_FONT);
        filterLabel.setForeground(TEXT_COLOR_DARK);
        filterPanel.add(filterLabel);

        filterTypeComboBox = new JComboBox<>(new String[]{"Tous Types", "Débit", "Crédit"});
        filterTypeComboBox.setFont(DATA_FONT);
        filterTypeComboBox.setBackground(Color.WHITE);
        filterPanel.add(filterTypeComboBox);

        filterCategoryComboBox = new JComboBox<>(new String[]{"Toutes Catégories", "Général", "Nourriture", "Transport", "Loisirs", "Salaire", "Autre"});
        filterCategoryComboBox.setFont(DATA_FONT);
        filterCategoryComboBox.setBackground(Color.WHITE);
        filterPanel.add(filterCategoryComboBox);

        JButton applyFilterButton = new JButton("Appliquer Filtre");
        applyFilterButton.setFont(LABEL_FONT);
        applyFilterButton.setForeground(Color.WHITE);
        applyFilterButton.setFocusPainted(false);
        applyFilterButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        applyFilterButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyHoverEffect(applyFilterButton, SECONDARY_COLOR, SECONDARY_COLOR.brighter());
        filterPanel.add(applyFilterButton);
        topTablePanel.add(filterPanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        searchPanel.setBackground(PANEL_BACKGROUND_COLOR);
        JLabel searchLabel = new JLabel("Rechercher:");
        searchLabel.setFont(LABEL_FONT);
        searchLabel.setForeground(TEXT_COLOR_DARK);
        searchPanel.add(searchLabel);
        searchField = new JTextField(18);
        searchField.setFont(DATA_FONT);
        searchPanel.add(searchField);
        JButton searchButton = new JButton("Rechercher");
        searchButton.setFont(LABEL_FONT);
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyHoverEffect(searchButton, SECONDARY_COLOR, SECONDARY_COLOR.brighter());
        searchPanel.add(searchButton);
        topTablePanel.add(searchPanel, BorderLayout.EAST);


        // Tableau des transactions
        String[] columnNames = {"ID", "Date", "Compte", "Type", "Catégorie", "Description", "Montant"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transactionsTable = new JTable(tableModel);
        transactionsTable.setFont(DATA_FONT);
        transactionsTable.setRowHeight(30);
        transactionsTable.getTableHeader().setFont(LABEL_FONT.deriveFont(Font.BOLD, 16));
        transactionsTable.getTableHeader().setBackground(SECONDARY_COLOR);
        transactionsTable.getTableHeader().setForeground(Color.WHITE);
        transactionsTable.setGridColor(new Color(235, 235, 235));
        transactionsTable.setSelectionBackground(new Color(173, 216, 230, 80));
        transactionsTable.setFillsViewportHeight(true);

        // Rendu personnalisé pour la colonne Montant
        transactionsTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String type = (String) table.getModel().getValueAt(row, 3);
                if (type.equals("Crédit")) {
                    c.setForeground(ACCENT_COLOR_POSITIVE);
                } else {
                    c.setForeground(ACCENT_COLOR_NEGATIVE);
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                setFont(MONETARY_FONT);
                return c;
            }
        });
        // Ajuster la largeur de la colonne ID
        transactionsTable.getColumnModel().getColumn(0).setMaxWidth(50);
        transactionsTable.getColumnModel().getColumn(0).setPreferredWidth(50);

        JScrollPane scrollPane = new JScrollPane(transactionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        displayPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel des actions sur les transactions (Modifier, Supprimer, Exporter)
        JPanel transactionActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        transactionActionsPanel.setBackground(PANEL_BACKGROUND_COLOR);
        transactionActionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(new Color(220, 220, 220))
        ));
        displayPanel.add(transactionActionsPanel, BorderLayout.SOUTH);

        JButton editButton = new JButton("Modifier");
        editButton.setFont(LABEL_FONT);
        editButton.setForeground(Color.WHITE);
        editButton.setFocusPainted(false);
        editButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Using a slightly different blue for edit button, so define hover color based on it
        Color editButtonColor = new Color(52, 152, 219);
        applyHoverEffect(editButton, editButtonColor, editButtonColor.brighter());
        transactionActionsPanel.add(editButton);

        JButton deleteButton = new JButton("Supprimer");
        deleteButton.setFont(LABEL_FONT);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyHoverEffect(deleteButton, ACCENT_COLOR_NEGATIVE, ACCENT_COLOR_NEGATIVE.brighter());
        transactionActionsPanel.add(deleteButton);

        JButton exportButton = new JButton("Exporter CSV");
        exportButton.setFont(LABEL_FONT);
        exportButton.setForeground(Color.WHITE);
        exportButton.setFocusPainted(false);
        exportButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Using a custom grey color for export button
        Color exportButtonColor = new Color(149, 165, 166);
        applyHoverEffect(exportButton, exportButtonColor, exportButtonColor.brighter());
        transactionActionsPanel.add(exportButton);


        // --- Actions des boutons ---
        addTransactionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String account = (String) accountComboBox.getSelectedItem();
                String type = (String) typeComboBox.getSelectedItem();
                String category = (String) categoryComboBox.getSelectedItem();
                String description = descriptionField.getText().trim();
                String montantText = montantField.getText().trim();

                if (description.isEmpty() || montantText.isEmpty()) {
                    JOptionPane.showMessageDialog(BeyahCompta.this,
                            "Veuillez remplir tous les champs.", "Erreur de saisie",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    double montant = Double.parseDouble(montantText);
                    if (montant <= 0) {
                        JOptionPane.showMessageDialog(BeyahCompta.this,
                                "Le montant doit être positif.", "Erreur de saisie",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    Transaction newTransaction = new Transaction(account, type, category, description, montant);
                    transactions.add(newTransaction);
                    updateAccountBalance(account, type, montant);
                    updateUI();
                    clearTransactionFields();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(BeyahCompta.this,
                            "Montant invalide. Veuillez entrer un nombre valide.", "Erreur de saisie",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        applyFilterButton.addActionListener(_ -> updateUI());
        searchButton.addActionListener(_ -> updateUI());
        searchField.addActionListener(_ -> updateUI());

        editButton.addActionListener(_ -> editTransaction());
        deleteButton.addActionListener(_ -> deleteSelectedTransaction());
        exportButton.addActionListener(_ -> exportTransactionsToCSV());

        return panel;
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel reportHeader = new JLabel("Résumé des Comptes", SwingConstants.CENTER);
        reportHeader.setFont(HEADER_FONT.deriveFont(Font.BOLD, 28));
        reportHeader.setForeground(TEXT_COLOR_DARK);
        panel.add(reportHeader, BorderLayout.NORTH);

        JPanel summaryPanel = new JPanel(new GridLayout(2, 2, 25, 25));
        summaryPanel.setBackground(PANEL_BACKGROUND_COLOR);
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(new Color(220, 220, 220))
        ));

        JLabel totalCreditTitle = new JLabel("Total des Crédits:", SwingConstants.CENTER);
        totalCreditTitle.setFont(SUBHEADER_FONT);
        totalCreditTitle.setForeground(TEXT_COLOR_DARK);
        summaryPanel.add(totalCreditTitle);

        totalCreditLabel = new JLabel(formatCurrency(0.0), SwingConstants.CENTER);
        totalCreditLabel.setFont(MONETARY_FONT.deriveFont(Font.BOLD, 26));
        totalCreditLabel.setForeground(ACCENT_COLOR_POSITIVE);
        summaryPanel.add(totalCreditLabel);

        JLabel totalDebitTitle = new JLabel("Total des Débits:", SwingConstants.CENTER);
        totalDebitTitle.setFont(SUBHEADER_FONT);
        totalDebitTitle.setForeground(TEXT_COLOR_DARK);
        summaryPanel.add(totalDebitTitle);

        totalDebitLabel = new JLabel(formatCurrency(0.0), SwingConstants.CENTER);
        totalDebitLabel.setFont(MONETARY_FONT.deriveFont(Font.BOLD, 26));
        totalDebitLabel.setForeground(ACCENT_COLOR_NEGATIVE);
        summaryPanel.add(totalDebitLabel);

        panel.add(summaryPanel, BorderLayout.CENTER);

        // Panel pour les soldes par compte
        accountBalancesPanel = new JPanel();
        accountBalancesPanel.setLayout(new BoxLayout(accountBalancesPanel, BoxLayout.Y_AXIS));
        accountBalancesPanel.setBackground(PANEL_BACKGROUND_COLOR);
        accountBalancesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)), "Soldes par Compte",
                        javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, SUBHEADER_FONT, TEXT_COLOR_DARK),
                new EmptyBorder(15, 15, 15, 15)
        ));
        JScrollPane accountScroll = new JScrollPane(accountBalancesPanel);
        accountScroll.setPreferredSize(new Dimension(300, 180));
        accountScroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(accountScroll, BorderLayout.SOUTH);

        return panel;
    }

    private double getGlobalBalance() {
        return accountBalances.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private void updateAccountBalance(String account, String type, double amount) {
        double currentBalance = accountBalances.getOrDefault(account, 0.0);
        if (type.equals("Débit")) {
            currentBalance -= amount;
        } else {
            currentBalance += amount;
        }
        accountBalances.put(account, currentBalance);
    }

    private void clearTransactionFields() {
        descriptionField.setText("");
        montantField.setText("");
        typeComboBox.setSelectedIndex(0);
        categoryComboBox.setSelectedIndex(0);
        accountComboBox.setSelectedIndex(0);
    }

    private void updateUI() {
        tableModel.setRowCount(0);

        String selectedTypeFilter = (String) filterTypeComboBox.getSelectedItem();
        String selectedCategoryFilter = (String) filterCategoryComboBox.getSelectedItem();
        String searchText = searchField.getText().trim().toLowerCase();

        List<Transaction> filteredTransactions = transactions.stream()
                .filter(t -> selectedTypeFilter.equals("Tous Types") || t.getType().equals(selectedTypeFilter))
                .filter(t -> selectedCategoryFilter.equals("Toutes Catégories") || t.getCategory().toLowerCase().contains(selectedCategoryFilter.toLowerCase()))
                .filter(t -> searchText.isEmpty() || t.getDescription().toLowerCase().contains(searchText) || t.getAccount().toLowerCase().contains(searchText) || t.getCategory().toLowerCase().contains(searchText))
                .collect(Collectors.toList());


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Transaction t : filteredTransactions) {
            tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getDate().format(formatter),
                    t.getAccount(),
                    t.getType(),
                    t.getCategory(),
                    t.getDescription(),
                    formatCurrency(t.getMontant())
            });
        }
        double globalBalance = getGlobalBalance();
        soldeGlobalLabel.setText("Solde Global: " + formatCurrency(globalBalance));
        if (globalBalance < 0) {
            soldeGlobalLabel.setForeground(ACCENT_COLOR_NEGATIVE);
        } else {
            soldeGlobalLabel.setForeground(Color.WHITE);
        }

        double totalDebit = transactions.stream()
                .filter(t -> t.getType().equals("Débit"))
                .mapToDouble(Transaction::getMontant)
                .sum();
        double totalCredit = transactions.stream()
                .filter(t -> t.getType().equals("Crédit"))
                .mapToDouble(Transaction::getMontant)
                .sum();

        totalDebitLabel.setText(formatCurrency(totalDebit));
        totalCreditLabel.setText(formatCurrency(totalCredit));

        accountBalancesPanel.removeAll();
        for (Map.Entry<String, Double> entry : accountBalances.entrySet()) {
            JLabel accountLabel = new JLabel(entry.getKey() + ": " + formatCurrency(entry.getValue()));
            accountLabel.setFont(DATA_FONT.deriveFont(Font.BOLD, 16));
            accountLabel.setBorder(new EmptyBorder(7, 0, 7, 0));
            if (entry.getValue() < 0) {
                accountLabel.setForeground(ACCENT_COLOR_NEGATIVE);
            } else {
                accountLabel.setForeground(TEXT_COLOR_DARK);
            }
            accountBalancesPanel.add(accountLabel);
        }
        accountBalancesPanel.revalidate();
        accountBalancesPanel.repaint();
    }

    private void editTransaction() {
        int selectedRow = transactionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner une transaction à modifier.", "Aucune sélection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        long transactionId = (Long) tableModel.getValueAt(selectedRow, 0);
        Transaction transactionToEdit = null;
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId() == transactionId) {
                transactionToEdit = transactions.get(i);
                break;
            }
        }

        if (transactionToEdit == null) {
            JOptionPane.showMessageDialog(this,
                    "Transaction non trouvée.", "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField editDescriptionField = new JTextField(transactionToEdit.getDescription());
        JTextField editMontantField = new JTextField(String.valueOf(transactionToEdit.getMontant()));
        JComboBox<String> editTypeComboBox = new JComboBox<>(new String[]{"Débit", "Crédit"});
        editTypeComboBox.setSelectedItem(transactionToEdit.getType());
        JComboBox<String> editCategoryComboBox = new JComboBox<>(new String[]{"Général", "Nourriture", "Transport", "Loisirs", "Salaire", "Autre"});
        editCategoryComboBox.setSelectedItem(transactionToEdit.getCategory());
        JComboBox<String> editAccountComboBox = new JComboBox<>(accountBalances.keySet().toArray(new String[0]));
        editAccountComboBox.setSelectedItem(transactionToEdit.getAccount());


        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Compte:"));
        panel.add(editAccountComboBox);
        panel.add(new JLabel("Type:"));
        panel.add(editTypeComboBox);
        panel.add(new JLabel("Catégorie:"));
        panel.add(editCategoryComboBox);
        panel.add(new JLabel("Description:"));
        panel.add(editDescriptionField);
        panel.add(new JLabel("Montant:"));
        panel.add(editMontantField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Modifier Transaction",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String newAccount = (String) editAccountComboBox.getSelectedItem();
                String newType = (String) editTypeComboBox.getSelectedItem();
                String newCategory = (String) editCategoryComboBox.getSelectedItem();
                String newDescription = editDescriptionField.getText().trim();
                double newMontant = Double.parseDouble(editMontantField.getText().trim());

                if (newDescription.isEmpty() || newMontant <= 0) {
                    JOptionPane.showMessageDialog(this,
                            "Veuillez remplir tous les champs et le montant doit être positif.", "Erreur de saisie",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Revert the old transaction's impact
                double oldMontant = transactionToEdit.getMontant();
                String oldType = transactionToEdit.getType();
                String oldAccount = transactionToEdit.getAccount();
                if (oldType.equals("Débit")) {
                    updateAccountBalance(oldAccount, "Crédit", oldMontant);
                } else { // oldType.equals("Crédit")
                    updateAccountBalance(oldAccount, "Débit", oldMontant);
                }

                // Update the transaction object
                transactionToEdit.setAccount(newAccount);
                transactionToEdit.setType(newType);
                transactionToEdit.setCategory(newCategory);
                transactionToEdit.setDescription(newDescription);
                transactionToEdit.setMontant(newMontant);

                // Apply the new transaction's impact
                updateAccountBalance(newAccount, newType, newMontant);

                updateUI();
                JOptionPane.showMessageDialog(this, "Transaction modifiée avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Montant invalide. Veuillez entrer un nombre valide.", "Erreur de saisie",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedTransaction() {
        int selectedRow = transactionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner une transaction à supprimer.", "Aucune sélection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Êtes-vous sûr de vouloir supprimer cette transaction ?", "Confirmer la suppression",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            long transactionId = (Long) tableModel.getValueAt(selectedRow, 0);
            Transaction transactionToDelete = null;
            int transactionIndex = -1;
            for (int i = 0; i < transactions.size(); i++) {
                if (transactions.get(i).getId() == transactionId) {
                    transactionToDelete = transactions.get(i);
                    transactionIndex = i;
                    break;
                }
            }

            if (transactionToDelete != null) {
                double amount = transactionToDelete.getMontant();
                String type = transactionToDelete.getType();
                String account = transactionToDelete.getAccount();

                // Revert the transaction's impact on the balance
                if (type.equals("Débit")) {
                    updateAccountBalance(account, "Crédit", amount); // Add back the debited amount
                } else { // type.equals("Crédit")
                    updateAccountBalance(account, "Débit", amount); // Subtract the credited amount
                }

                transactions.remove(transactionIndex);
                updateUI();
                JOptionPane.showMessageDialog(this, "Transaction supprimée.", "Succès", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void exportTransactionsToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Enregistrer les transactions au format CSV");
        String userHome = System.getProperty("user.home");
        java.io.File documentsDir = new java.io.File(userHome, "Documents");
        if (documentsDir.exists() && documentsDir.isDirectory()) {
            fileChooser.setCurrentDirectory(documentsDir);
        } else {
            fileChooser.setCurrentDirectory(new java.io.File(userHome));
        }

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
            }
            // Use the private saveTransactions method
            saveTransactions(filePath, true); // True to indicate user-initiated export
        }
    }

    /**
     * Saves transaction data to a specified CSV file.
     * @param filePath The path to the CSV file.
     * @param isUserExport True if this is a user-initiated export, false for automatic persistence.
     */
    private void saveTransactions(String filePath, boolean isUserExport) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write CSV header
            writer.append("\"ID\",\"Date\",\"Compte\",\"Type\",\"Catégorie\",\"Description\",\"Montant\"");
            writer.newLine();

            // Write CSV data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Transaction t : transactions) {
                writer.append("\"").append(String.valueOf(t.getId())).append("\"").append(",");
                writer.append("\"").append(t.getDate().format(formatter)).append("\"").append(",");
                writer.append("\"").append(escapeCsv(t.getAccount())).append("\"").append(",");
                writer.append("\"").append(escapeCsv(t.getType())).append("\"").append(",");
                writer.append("\"").append(escapeCsv(t.getCategory())).append("\"").append(",");
                writer.append("\"").append(escapeCsv(t.getDescription())).append("\"").append(",");
                writer.append("\"").append(String.valueOf(t.getMontant())).append("\"");
                writer.newLine();
            }
            if (isUserExport) {
                JOptionPane.showMessageDialog(this, "Transactions exportées avec succès vers " + filePath, "Exportation réussie", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            if (isUserExport) {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'exportation: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            } else {
                System.err.println("Erreur lors de la sauvegarde automatique des transactions: " + ex.getMessage());
            }
            ex.printStackTrace();
        }
    }

    /**
     * Escapes a string for CSV output by enclosing it in double quotes and escaping existing double quotes.
     * @param value The string to escape.
     * @return The escaped string.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    /**
     * Saves account balances to a text file.
     */
    private void saveAccountBalances() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BALANCES_FILE))) {
            for (Map.Entry<String, Double> entry : accountBalances.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException ex) {
            System.err.println("Erreur lors de la sauvegarde des soldes de compte: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Loads transaction data from the CSV file.
     */
    private void loadTransactions() {
        File file = new File(TRANSACTIONS_FILE);
        if (!file.exists()) {
            System.out.println("Fichier de transactions non trouvé. Démarrage avec des transactions vides.");
            return;
        }

        transactions.clear(); // Clear existing data before loading
        long maxId = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] parts = parseCsvLine(line); // Custom CSV parsing to handle commas in descriptions
                if (parts.length == 7) {
                    try {
                        long id = Long.parseLong(parts[0]);
                        LocalDate date = LocalDate.parse(parts[1], formatter);
                        String account = parts[2];
                        String type = parts[3];
                        String category = parts[4];
                        String description = parts[5];
                        double montant = Double.parseDouble(parts[6]);

                        Transaction loadedTransaction = new Transaction(id, date, account, type, category, description, montant);
                        transactions.add(loadedTransaction);
                        if (id > maxId) {
                            maxId = id;
                        }
                    } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                        System.err.println("Erreur lors de la lecture d'une ligne de transaction: " + line + " - " + e.getMessage());
                    }
                }
            }
            // Directly set the nextId, as it's now public static
            Transaction.nextId = maxId + 1;
            System.out.println("Transactions chargées avec succès. Prochain ID: " + Transaction.nextId);
        } catch (IOException ex) {
            System.err.println("Erreur lors du chargement des transactions: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Parses a CSV line, handling quoted fields with embedded commas.
     * Simple parsing, assumes no escaped quotes *within* a quoted field beyond the "" for a single ".
     */
    private String[] parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder currentToken = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped double quote
                    currentToken.append('"');
                    i++; // Skip the next quote
                } else {
                    inQuote = !inQuote;
                }
            } else if (ch == ',' && !inQuote) {
                parts.add(currentToken.toString());
                currentToken = new StringBuilder();
            } else {
                currentToken.append(ch);
            }
        }
        parts.add(currentToken.toString()); // Add the last token
        return parts.toArray(new String[0]);
    }


    /**
     * Loads account balances from the text file.
     */
    private void loadAccountBalances() {
        File file = new File(BALANCES_FILE);
        if (!file.exists()) {
            System.out.println("Fichier de soldes de compte non trouvé. Démarrage avec les soldes par défaut.");
            return;
        }

        accountBalances.clear(); // Clear default balances before loading
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    try {
                        String accountName = parts[0];
                        double balance = Double.parseDouble(parts[1]);
                        accountBalances.put(accountName, balance);
                    } catch (NumberFormatException e) {
                        System.err.println("Erreur lors de la lecture d'une ligne de solde: " + line + " - " + e.getMessage());
                    }
                }
            }
            // Ensure default accounts are present if not loaded from file (e.g., first run)
            accountBalances.putIfAbsent("Caisse", 0.0);
            accountBalances.putIfAbsent("Banque", 0.0);
            accountBalances.putIfAbsent("Épargne", 0.0);
            System.out.println("Soldes de compte chargés avec succès.");
        } catch (IOException ex) {
            System.err.println("Erreur lors du chargement des soldes de compte: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Orchestrates saving all application data.
     */
    private void saveData() {
        saveTransactions(TRANSACTIONS_FILE, false); // False for automatic persistence
        saveAccountBalances();
        System.out.println("Données sauvegardées.");
    }

    /**
     * Orchestrates loading all application data.
     */
    private void loadData() {
        loadAccountBalances(); // Load balances first, as transactions rely on accounts
        loadTransactions();
    }


    private String formatCurrency(double amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        return currencyFormat.format(amount);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new BeyahCompta().setVisible(true);
            }
        });
    }
}

class Transaction {
    // Rend `nextId` public static pour garantir la visibilité selon l'erreur rencontrée
    public static long nextId = 1;
    private long id;
    private LocalDate date;
    private String account;
    private String type;
    private String category;
    private String description;
    private double montant;

    // Constructor for new transactions (auto-generates ID and date)
    public Transaction(String account, String type, String category, String description, double montant) {
        this.id = nextId++;
        this.date = LocalDate.now();
        this.account = account;
        this.type = type;
        this.category = category;
        this.description = description;
        this.montant = montant;
    }

    // Constructor for loading existing transactions (allows setting ID and date)
    public Transaction(long id, LocalDate date, String account, String type, String category, String description, double montant) {
        this.id = id;
        this.date = date;
        this.account = account;
        this.type = type;
        this.category = category;
        this.description = description;
        this.montant = montant;
    }

    public long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getAccount() { return account; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getMontant() { return montant; }

    public void setAccount(String account) { this.account = account; }
    public void setType(String type) { this.type = type; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setMontant(double montant) { this.montant = montant; }

    // La méthode setNextId n'est plus nécessaire car nextId est directement accessible
    // et sera mis à jour après le chargement des transactions.
    // public static void setNextId(long newNextId) {
    //     if (newNextId > nextId) {
    //         nextId = newNextId;
    //     }
    // }
}