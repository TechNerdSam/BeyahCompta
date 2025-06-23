import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent; // Import ActionEvent
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BeyahCompta is a Swing-based financial management application designed for clean and efficient tracking
 * of transactions, account balances, and budgeting. It provides features such as adding, editing, deleting,
 * filtering, searching transactions, and viewing financial reports including a pie chart visualization of expenses.
 *
 * <p>BeyahCompta est une application de gestion financière basée sur Swing, conçue pour un suivi propre
 * et efficace des transactions, des soldes de compte et de la budgétisation. Elle offre des fonctionnalités
 * telles que l'ajout, la modification, la suppression, le filtrage et la recherche de transactions,
 * ainsi que la visualisation de rapports financiers, y compris un graphique circulaire des dépenses.</p>
 */
public class BeyahCompta extends JFrame { // JFrame already implements Serializable, so no need to declare it again.

    private static final long serialVersionUID = 1L; // Serial Version UID for compatibility / UID de version série pour la compatibilité

    // --- Application Constants / Constantes de l'application ---
    private static final String APP_TITLE = "BéyahCompta - Gestion Financière Épurée";
    private static final String DATA_DIR_NAME = "BeyahComptaData";
    private static final String TRANSACTIONS_FILE_SER = "transactions.ser";
    private static final String BALANCES_AND_BUDGETS_FILE_SER = "data.ser"; // Combines account balances and budgets
    private static final String BACKUP_EXTENSION = ".bak";

    // Default accounts / Comptes par défaut
    private static final String DEFAULT_ACCOUNT_CASH = "Caisse";
    private static final String DEFAULT_ACCOUNT_BANK = "Banque";
    private static final String DEFAULT_ACCOUNT_SAVINGS = "Épargne";

    // Default categories / Catégories par défaut
    private static final String CATEGORY_GENERAL = "Général";
    private static final String CATEGORY_FOOD = "Nourriture";
    private static final String CATEGORY_TRANSPORT = "Transport";
    private static final String CATEGORY_LEISURE = "Loisirs";
    private static final String CATEGORY_SALARY = "Salaire";
    private static final String CATEGORY_OTHER = "Autre";

    // UI Texts / Textes de l'interface utilisateur
    private static final String TAB_TRANSACTIONS = "Transactions";
    private static final String TAB_REPORTS = "Rapports";
    private static final String HEADER_NEW_TRANSACTION = "Nouvelle Transaction";
    private static final String LABEL_ACCOUNT = "Compte:";
    private static final String LABEL_TYPE = "Type:";
    private static final String LABEL_CATEGORY = "Catégorie:";
    private static final String LABEL_DESCRIPTION = "Description:";
    private static final String LABEL_AMOUNT = "Montant:";
    private static final String BUTTON_ADD_TRANSACTION = "Ajouter Transaction";
    private static final String LABEL_FILTER_BY = "Filtrer par:";
    private static final String LABEL_SEARCH = "Rechercher:";
    private static final String BUTTON_APPLY_FILTER = "Appliquer Filtre";
    private static final String BUTTON_SEARCH = "Rechercher";
    private static final String BUTTON_EDIT = "Modifier";
    private static final String BUTTON_DELETE = "Supprimer";
    private static final String BUTTON_EXPORT_CSV = "Exporter CSV";
    private static final String REPORT_HEADER_TITLE = "Résumé des Comptes et Budgets";
    private static final String REPORT_TOTAL_CREDIT_TITLE = "Total des Crédits:";
    private static final String REPORT_TOTAL_DEBIT_TITLE = "Total des Débits:";
    private static final String REPORT_ACCOUNT_BALANCES_TITLE = "Soldes par Compte";
    private static final String REPORT_BUDGET_SUMMARY_TITLE = "Résumé Budgétaire par Catégorie";
    private static final String REPORT_EXPENSE_PIE_CHART_TITLE = "Dépenses par Catégorie";
    private static final String BUTTON_MANAGE_BUDGETS = "Gérer les Budgets";

    // Error/Warning Messages / Messages d'erreur/avertissement
    private static final String MSG_WARNING_EMPTY_FIELDS = "Veuillez remplir tous les champs.";
    private static final String MSG_WARNING_POSITIVE_AMOUNT = "Le montant doit être positif.";
    private static final String MSG_ERROR_INVALID_AMOUNT = "Montant invalide. Veuillez entrer un nombre valide.";
    private static final String MSG_WARNING_NO_SELECTION = "Veuillez sélectionner une transaction à modifier/supprimer.";
    private static final String MSG_ERROR_TRANSACTION_NOT_FOUND = "Transaction non trouvée.";
    private static final String MSG_CONFIRM_DELETE = "Êtes-vous sûr de vouloir supprimer cette transaction ?";
    private static final String MSG_SUCCESS_TRANSACTION_MODIFIED = "Transaction modifiée avec succès.";
    private static final String MSG_SUCCESS_TRANSACTION_DELETED = "Transaction supprimée.";
    private static final String MSG_SUCCESS_EXPORT = "Transactions exportées avec succès vers ";
    private static final String MSG_ERROR_EXPORT = "Erreur lors de l'exportation: ";
    private static final String MSG_SUCCESS_BUDGET_UPDATED = "Budgets mis à jour avec succès.";
    private static final String MSG_WARNING_BUDGET_POSITIVE = "Le budget doit être positif ou nul.";
    private static final String MSG_ERROR_INVALID_BUDGET_AMOUNT = "Montant de budget invalide. Veuillez entrer un nombre valide.";
    private static final String MSG_NO_EXPENSE_DATA = "Aucune dépense enregistrée.";


    // --- UI Components / Composants de l'interface utilisateur ---
    private JTable transactionsTable;
    private DefaultTableModel tableModel;
    private JLabel soldeGlobalLabel;
    private JComboBox<String> filterTypeComboBox;
    private JComboBox<String> filterCategoryComboBox;
    private JTextField searchField;

    // Report-related components / Composants liés aux rapports
    private JLabel totalDebitLabel;
    private JLabel totalCreditLabel;
    private JPanel accountBalancesPanel;
    private JPanel budgetSummaryPanel;
    private PieChartPanel pieChartPanel; // Direct reference for robustness

    // Transaction input fields / Champs de saisie de transaction
    private JTextField descriptionField;
    private JTextField montantField;
    private JComboBox<TransactionType> typeComboBox; // Using enum
    private JComboBox<TransactionCategory> categoryComboBox; // Using enum
    private JComboBox<String> accountComboBox;

    // --- Application Data / Données de l'application ---
    private List<Transaction> transactions;
    private Map<String, Double> accountBalances;
    private Map<TransactionCategory, Double> budgets; // Using enum for keys

    // File paths for persistence / Chemins de fichiers pour la persistance
    private final String dataDirPath;
    private final String transactionsFileSer;
    private final String balancesAndBudgetsFileSer;
    private final String transactionsFileSerBackup;
    private final String balancesAndBudgetsFileSerBackup;


    // --- UI Colors and Fonts (Material Design inspired) / Couleurs et polices de l'interface (inspirées du Material Design) ---
    private final Color PRIMARY_COLOR = new Color(3, 169, 244); // Light Blue 500
    private final Color SECONDARY_COLOR = new Color(21, 101, 192); // Blue 800
    private final Color ACCENT_COLOR_POSITIVE = new Color(76, 175, 80); // Green 500
    private final Color ACCENT_COLOR_NEGATIVE = new Color(244, 67, 54); // Red 500
    private final Color BACKGROUND_COLOR = new Color(236, 239, 241); // Blue Grey 50
    private final Color PANEL_BACKGROUND_COLOR = Color.WHITE;
    private final Color TEXT_COLOR_DARK = new Color(33, 33, 33); // Grey 900

    private final String FONT_NAME = "Segoe UI"; // System-friendly sans-serif font
    private final Font APP_TITLE_HEADER_FONT = new Font("Script MT Bold", Font.BOLD, 36);
    private final Font HEADER_FONT = new Font(FONT_NAME, Font.BOLD, 32);
    private final Font SUBHEADER_FONT = new Font(FONT_NAME, Font.BOLD, 20);
    private final Font LABEL_FONT = new Font(FONT_NAME, Font.BOLD, 15);
    private final Font DATA_FONT = new Font(FONT_NAME, Font.PLAIN, 14);
    private final Font MONETARY_FONT = new Font(FONT_NAME, Font.BOLD, 16);

    /**
     * Constructor for the BeyahCompta application.
     * Initializes UI, loads data, and sets up event listeners.
     *
     * <p>Constructeur de l'application BeyahCompta.
     * Initialise l'interface utilisateur, charge les données et configure les écouteurs d'événements.</p>
     */
    public BeyahCompta() {
        // Initialize file paths for persistence / Initialisation des chemins de fichiers pour la persistance
        String userHome = System.getProperty("user.home");
        this.dataDirPath = userHome + File.separator + DATA_DIR_NAME;
        this.transactionsFileSer = this.dataDirPath + File.separator + TRANSACTIONS_FILE_SER;
        this.balancesAndBudgetsFileSer = this.dataDirPath + File.separator + BALANCES_AND_BUDGETS_FILE_SER;
        this.transactionsFileSerBackup = this.transactionsFileSer + BACKUP_EXTENSION;
        this.balancesAndBudgetsFileSerBackup = this.balancesAndBudgetsFileSer + BACKUP_EXTENSION;

        // Create data directory if it doesn't exist / Créer le répertoire de données s'il n'existe pas
        new File(this.dataDirPath).mkdirs();

        this.transactions = new ArrayList<>();
        this.accountBalances = new HashMap<>();
        this.budgets = new HashMap<>();

        // Initialize default account balances if no balance file exists
        // Initialiser les soldes des comptes par défaut si aucun fichier de solde n'existe
        this.accountBalances.put(DEFAULT_ACCOUNT_CASH, 0.0);
        this.accountBalances.put(DEFAULT_ACCOUNT_BANK, 0.0);
        this.accountBalances.put(DEFAULT_ACCOUNT_SAVINGS, 0.0);

        // Initialize default budgets for each category
        // Initialiser les budgets par défaut pour chaque catégorie
        Arrays.stream(TransactionCategory.values())
              .filter(cat -> cat != TransactionCategory.SALAIRE) // Salary is an income, not typically budgeted
              .forEach(cat -> this.budgets.putIfAbsent(cat, 0.0));

        // Load persistent data at startup / Charger les données persistantes au démarrage
        loadData();

        setTitle(APP_TITLE);
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window / Centrer la fenêtre
        setLayout(new BorderLayout(20, 20)); // Generous margins for the overall layout / Marges généreuses pour la mise en page globale

        // --- Apply Nimbus Look and Feel --- / --- Appliquer le Look and Feel Nimbus ---
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            System.err.println("Failed to initialize Nimbus Look and Feel. Falling back to default L&F.");
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                System.err.println("Failed to set cross-platform L&F. " + ex.getMessage());
            }
        }
        // Visual adjustments for Nimbus L&F with new colors
        // Ajustements visuels pour le L&F Nimbus avec les nouvelles couleurs
        UIManager.put("control", BACKGROUND_COLOR);
        UIManager.put("info", PANEL_BACKGROUND_COLOR);
        UIManager.put("nimbusBase", PRIMARY_COLOR);
        UIManager.put("nimbusBlueGrey", SECONDARY_COLOR);
        UIManager.put("text", TEXT_COLOR_DARK);

        // --- Global Header --- / --- En-tête Global ---
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

        // --- Central Content with Tabs --- / --- Contenu Central avec Onglets ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(LABEL_FONT);
        tabbedPane.setBackground(BACKGROUND_COLOR);
        tabbedPane.setForeground(TEXT_COLOR_DARK);
        // Customizing tabs for Nimbus / Personnalisation des onglets pour Nimbus
        UIManager.put("TabbedPane.contentAreaColor", BACKGROUND_COLOR);
        UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 15, 8, 15));
        UIManager.put("TabbedPane.selectedBackground", PRIMARY_COLOR);
        UIManager.put("TabbedPane.unselectedBackground", SECONDARY_COLOR.brighter());
        UIManager.put("TabbedPane.unselectedForeground", TEXT_COLOR_DARK);
        UIManager.put("TabbedPane.selected", PRIMARY_COLOR);

        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addTab(TAB_TRANSACTIONS, createTransactionsPanel());
        tabbedPane.addTab(TAB_REPORTS, createReportsPanel());

        updateUI(null); // Initialize display. Pass null for the ActionEvent since it's an initial call.

        // Save data when the application closes / Sauvegarder les données à la fermeture de l'application
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent _e) { // Use _ for unused parameter / Utiliser _ pour le paramètre non utilisé
                saveData();
            }
        });

        // Make the JFrame visible / Rendre la JFrame visible
        setVisible(true); // <--- Ligne ajoutée
    }

    /**
     * Applies hover effects to a JButton, changing its background color on mouse entry/exit.
     *
     * <p>Applique des effets de survol à un JButton, changeant sa couleur d'arrière-plan
     * à l'entrée et à la sortie de la souris.</p>
     *
     * @param button The JButton to apply the effect to. / Le JButton auquel appliquer l'effet.
     * @param normalColor The button's normal background color. / La couleur d'arrière-plan normale du bouton.
     * @param hoverColor The button's background color on hover. / La couleur d'arrière-plan du bouton au survol.
     */
    private void applyHoverEffect(JButton button, Color normalColor, Color hoverColor) {
        button.setBackground(normalColor);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent _evt) { // Use _ for unused parameter / Utiliser _ pour le paramètre non utilisé
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent _evt) { // Use _ for unused parameter / Utiliser _ pour le paramètre non utilisé
                button.setBackground(normalColor);
            }
        });
    }

    /**
     * Creates and configures the transactions panel, including input fields, table, and action buttons.
     *
     * <p>Crée et configure le panneau des transactions, incluant les champs de saisie, le tableau
     * et les boutons d'action.</p>
     *
     * @return A JPanel containing the transactions management interface. / Un JPanel contenant l'interface de gestion des transactions.
     */
    private JPanel createTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_COLOR);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBackground(BACKGROUND_COLOR);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(splitPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(createInputPanel());
        splitPane.setRightComponent(createDisplayPanel());

        return panel;
    }

    /**
     * Creates the input panel for new transactions.
     *
     * <p>Crée le panneau de saisie pour les nouvelles transactions.</p>
     *
     * @return A JPanel with input fields and an "Add Transaction" button. / Un JPanel avec des champs de saisie et un bouton "Ajouter Transaction".
     */
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(PANEL_BACKGROUND_COLOR);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(25, 25, 25, 25),
                BorderFactory.createLineBorder(new Color(220, 220, 220))
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel inputHeader = new JLabel(HEADER_NEW_TRANSACTION);
        inputHeader.setFont(SUBHEADER_FONT);
        inputHeader.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        inputPanel.add(inputHeader, gbc);

        gbc.gridwidth = 1;

        // Account / Compte
        gbc.gridy++;
        addLabeledComponent(inputPanel, LABEL_ACCOUNT, accountComboBox = new JComboBox<>(accountBalances.keySet().toArray(new String[0])), gbc);

        // Transaction Type / Type de transaction
        gbc.gridy++;
        typeComboBox = new JComboBox<>(TransactionType.values()); // Using Enum
        addLabeledComponent(inputPanel, LABEL_TYPE, typeComboBox, gbc);

        // Category / Catégorie
        gbc.gridy++;
        categoryComboBox = new JComboBox<>(TransactionCategory.values()); // Using Enum
        addLabeledComponent(inputPanel, LABEL_CATEGORY, categoryComboBox, gbc);

        // Description / Description
        gbc.gridy++;
        addLabeledComponent(inputPanel, LABEL_DESCRIPTION, descriptionField = new JTextField(20), gbc);

        // Amount / Montant
        gbc.gridy++;
        addLabeledComponent(inputPanel, LABEL_AMOUNT, montantField = new JTextField(10), gbc);

        // Add Transaction Button / Bouton Ajouter Transaction
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton addTransactionButton = new JButton(BUTTON_ADD_TRANSACTION);
        addTransactionButton.setFont(LABEL_FONT.deriveFont(Font.BOLD, 16));
        addTransactionButton.setForeground(Color.WHITE);
        addTransactionButton.setFocusPainted(false);
        addTransactionButton.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        addTransactionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyHoverEffect(addTransactionButton, PRIMARY_COLOR, PRIMARY_COLOR.brighter());
        inputPanel.add(addTransactionButton, gbc);

        // Action Listener for Add Transaction Button / Écouteur d'action pour le bouton Ajouter Transaction
        addTransactionButton.addActionListener(this::onAddTransaction);

        return inputPanel;
    }

    /**
     * Helper method to add a labeled component to a JPanel using GridBagLayout.
     *
     * <p>Méthode d'aide pour ajouter un composant étiqueté à un JPanel en utilisant GridBagLayout.</p>
     *
     * @param panel The panel to add components to. / Le panneau auquel ajouter les composants.
     * @param labelText The text for the label. / Le texte de l'étiquette.
     * @param component The component to add. / Le composant à ajouter.
     * @param gbc The GridBagConstraints object. / L'objet GridBagConstraints.
     */
    private void addLabeledComponent(JPanel panel, String labelText, JComponent component, GridBagConstraints gbc) {
        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0;
        panel.add(label, gbc);

        component.setFont(DATA_FONT);
        component.setBackground(Color.WHITE);
        component.setPreferredSize(new Dimension(200, 30)); // Consistent size / Taille cohérente
        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    /**
     * Handles the action for adding a new transaction. Performs input validation.
     *
     * <p>Gère l'action d'ajout d'une nouvelle transaction. Effectue la validation des entrées.</p>
     *
     * @param _e The ActionEvent (unused). / L'ActionEvent (non utilisé).
     */
    private void onAddTransaction(java.awt.event.ActionEvent _e) { // Use _ for unused parameter / Utiliser _ pour le paramètre non utilisé
        String account = (String) accountComboBox.getSelectedItem();
        TransactionType type = (TransactionType) typeComboBox.getSelectedItem();
        TransactionCategory category = (TransactionCategory) categoryComboBox.getSelectedItem();
        String description = descriptionField.getText().trim();
        String montantText = montantField.getText().trim();

        if (description.isEmpty() || montantText.isEmpty()) {
            JOptionPane.showMessageDialog(this, MSG_WARNING_EMPTY_FIELDS, "Erreur de saisie", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double montant = Double.parseDouble(montantText);
            if (montant <= 0) {
                JOptionPane.showMessageDialog(this, MSG_WARNING_POSITIVE_AMOUNT, "Erreur de saisie", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Transaction newTransaction = new Transaction(account, type, category, description, montant);
            transactions.add(newTransaction);
            updateAccountBalance(account, type, montant);
            updateUI(null); // Call updateUI with a null ActionEvent
            clearTransactionFields();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, MSG_ERROR_INVALID_AMOUNT, "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Creates the display panel for transactions, including filter/search options and the transaction table.
     *
     * <p>Crée le panneau d'affichage des transactions, y compris les options de filtre/recherche
     * et le tableau des transactions.</p>
     *
     * @return A JPanel containing the transaction display and management tools. / Un JPanel contenant l'affichage des transactions et les outils de gestion.
     */
    private JPanel createDisplayPanel() {
        JPanel displayPanel = new JPanel(new BorderLayout(15, 15));
        displayPanel.setBackground(BACKGROUND_COLOR);

        // Filter and Search Panel / Panneau de filtrage et de recherche
        JPanel topTablePanel = new JPanel(new BorderLayout(15, 15));
        topTablePanel.setBackground(PANEL_BACKGROUND_COLOR);
        topTablePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(new Color(220, 220, 220))
        ));
        displayPanel.add(topTablePanel, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        filterPanel.setBackground(PANEL_BACKGROUND_COLOR);
        JLabel filterLabel = new JLabel(LABEL_FILTER_BY);
        filterLabel.setFont(LABEL_FONT);
        filterLabel.setForeground(TEXT_COLOR_DARK);
        filterPanel.add(filterLabel);

        filterTypeComboBox = new JComboBox<>(Arrays.stream(TransactionType.values())
                .map(Enum::toString)
                .collect(Collectors.toCollection(ArrayList::new))
                .toArray(new String[0]));
        filterTypeComboBox.insertItemAt("Tous Types", 0); // Add "All Types" option / Ajouter l'option "Tous Types"
        filterTypeComboBox.setSelectedIndex(0); // Select "All Types" by default / Sélectionner "Tous Types" par défaut
        filterTypeComboBox.setFont(DATA_FONT);
        filterTypeComboBox.setBackground(Color.WHITE);
        filterPanel.add(filterTypeComboBox);

        // Get category names from enum for filter combo box / Obtenir les noms de catégorie de l'énumération pour la boîte de combo filtre
        List<String> filterCategories = Arrays.stream(TransactionCategory.values())
                .map(Enum::toString)
                .collect(Collectors.toCollection(ArrayList::new));
        filterCategories.add(0, "Toutes Catégories"); // Add "All Categories" option / Ajouter l'option "Toutes Catégories"
        filterCategoryComboBox = new JComboBox<>(filterCategories.toArray(new String[0]));
        filterCategoryComboBox.setFont(DATA_FONT);
        filterCategoryComboBox.setBackground(Color.WHITE);
        filterPanel.add(filterCategoryComboBox);

        JButton applyFilterButton = new JButton(BUTTON_APPLY_FILTER);
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
        JLabel searchLabel = new JLabel(LABEL_SEARCH);
        searchLabel.setFont(LABEL_FONT);
        searchLabel.setForeground(TEXT_COLOR_DARK);
        searchPanel.add(searchLabel);
        searchField = new JTextField(18);
        searchField.setFont(DATA_FONT);
        searchPanel.add(searchField);
        JButton searchButton = new JButton(BUTTON_SEARCH);
        searchButton.setFont(LABEL_FONT);
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyHoverEffect(searchButton, SECONDARY_COLOR, SECONDARY_COLOR.brighter());
        searchPanel.add(searchButton);
        topTablePanel.add(searchPanel, BorderLayout.EAST);


        // Transactions Table / Tableau des transactions
        String[] columnNames = {"ID", "Date", "Compte", "Type", "Catégorie", "Description", "Montant"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            // Ensure proper column class for sorting and rendering / Assurer une classe de colonne appropriée pour le tri et le rendu
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Long.class : // ID
                       columnIndex == 6 ? Double.class : // Montant
                       Object.class;
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

        // Custom renderer for Amount column / Rendu personnalisé pour la colonne Montant
        transactionsTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // Cast to String, then to Enum for type check / Convertir en String, puis en Enum pour la vérification de type
                String typeString = (String) table.getModel().getValueAt(row, 3);
                TransactionType type = TransactionType.fromString(typeString);
                if (type == TransactionType.CREDIT) {
                    c.setForeground(ACCENT_COLOR_POSITIVE);
                } else {
                    c.setForeground(ACCENT_COLOR_NEGATIVE);
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                setFont(MONETARY_FONT);
                return c;
            }
        });
        // Adjust ID column width / Ajuster la largeur de la colonne ID
        transactionsTable.getColumnModel().getColumn(0).setMaxWidth(50);
        transactionsTable.getColumnModel().getColumn(0).setPreferredWidth(50);

        JScrollPane scrollPane = new JScrollPane(transactionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        displayPanel.add(scrollPane, BorderLayout.CENTER);

        // Transaction Actions Panel (Edit, Delete, Export) / Panneau des actions de transaction (Modifier, Supprimer, Exporter)
        JPanel transactionActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        transactionActionsPanel.setBackground(PANEL_BACKGROUND_COLOR);
        transactionActionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(new Color(220, 220, 220))
        ));
        displayPanel.add(transactionActionsPanel, BorderLayout.SOUTH);

        JButton editButton = new JButton(BUTTON_EDIT);
        editButton.setFont(LABEL_FONT);
        editButton.setForeground(Color.WHITE);
        editButton.setFocusPainted(false);
        editButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Color editButtonColor = new Color(52, 152, 219); // Custom blue for edit / Bleu personnalisé pour modifier
        applyHoverEffect(editButton, editButtonColor, editButtonColor.brighter());
        transactionActionsPanel.add(editButton);

        JButton deleteButton = new JButton(BUTTON_DELETE);
        deleteButton.setFont(LABEL_FONT);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyHoverEffect(deleteButton, ACCENT_COLOR_NEGATIVE, ACCENT_COLOR_NEGATIVE.brighter());
        transactionActionsPanel.add(deleteButton);

        JButton exportButton = new JButton(BUTTON_EXPORT_CSV);
        exportButton.setFont(LABEL_FONT);
        exportButton.setForeground(Color.WHITE);
        exportButton.setFocusPainted(false);
        exportButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Color exportButtonColor = new Color(149, 165, 166); // Custom grey for export / Gris personnalisé pour exporter
        applyHoverEffect(exportButton, exportButtonColor, exportButtonColor.brighter());
        transactionActionsPanel.add(exportButton);

        // Action Listeners for management buttons / Écouteurs d'action pour les boutons de gestion
        applyFilterButton.addActionListener(this::updateUI);
        searchButton.addActionListener(this::updateUI);
        searchField.addActionListener(this::updateUI);

        editButton.addActionListener(this::editTransaction);
        deleteButton.addActionListener(this::deleteSelectedTransaction);
        exportButton.addActionListener(this::exportTransactionsToCSV);

        return displayPanel;
    }

    /**
     * Creates and configures the reports panel, including financial summaries, budget overview, and a pie chart.
     *
     * <p>Crée et configure le panneau des rapports, incluant les résumés financiers,
     * l'aperçu budgétaire et un graphique circulaire.</p>
     *
     * @return A JPanel containing various financial reports. / Un JPanel contenant divers rapports financiers.
     */
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel reportHeader = new JLabel(REPORT_HEADER_TITLE, SwingConstants.CENTER);
        reportHeader.setFont(HEADER_FONT.deriveFont(Font.BOLD, 28));
        reportHeader.setForeground(TEXT_COLOR_DARK);
        panel.add(reportHeader, BorderLayout.NORTH);

        JPanel mainReportContent = new JPanel(new GridLayout(1, 2, 25, 25)); // Two columns for summary and pie chart
        mainReportContent.setBackground(BACKGROUND_COLOR);

        // Left side: Summary of Credits/Debits and Account Balances / Côté gauche: Résumé des crédits/débits et soldes de compte
        JPanel financialSummaryPanel = new JPanel(new BorderLayout(20, 20));
        financialSummaryPanel.setBackground(BACKGROUND_COLOR);

        JPanel totalSummary = new JPanel(new GridLayout(2, 2, 25, 25));
        totalSummary.setBackground(PANEL_BACKGROUND_COLOR);
        totalSummary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(new Color(220, 220, 220))
        ));

        JLabel totalCreditTitle = new JLabel(REPORT_TOTAL_CREDIT_TITLE, SwingConstants.CENTER);
        totalCreditTitle.setFont(SUBHEADER_FONT);
        totalCreditTitle.setForeground(TEXT_COLOR_DARK);
        totalSummary.add(totalCreditTitle);

        totalCreditLabel = new JLabel(formatCurrency(0.0), SwingConstants.CENTER);
        totalCreditLabel.setFont(MONETARY_FONT.deriveFont(Font.BOLD, 26));
        totalCreditLabel.setForeground(ACCENT_COLOR_POSITIVE);
        totalSummary.add(totalCreditLabel);

        JLabel totalDebitTitle = new JLabel(REPORT_TOTAL_DEBIT_TITLE, SwingConstants.CENTER);
        totalDebitTitle.setFont(SUBHEADER_FONT);
        totalDebitTitle.setForeground(TEXT_COLOR_DARK);
        totalSummary.add(totalDebitTitle);

        totalDebitLabel = new JLabel(formatCurrency(0.0), SwingConstants.CENTER);
        totalDebitLabel.setFont(MONETARY_FONT.deriveFont(Font.BOLD, 26));
        totalDebitLabel.setForeground(ACCENT_COLOR_NEGATIVE);
        totalSummary.add(totalDebitLabel);
        financialSummaryPanel.add(totalSummary, BorderLayout.NORTH);

        // Panel for account balances / Panneau pour les soldes de compte
        accountBalancesPanel = new JPanel();
        accountBalancesPanel.setLayout(new BoxLayout(accountBalancesPanel, BoxLayout.Y_AXIS));
        accountBalancesPanel.setBackground(PANEL_BACKGROUND_COLOR);
        accountBalancesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)), REPORT_ACCOUNT_BALANCES_TITLE,
                        javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, SUBHEADER_FONT, TEXT_COLOR_DARK),
                new EmptyBorder(15, 15, 15, 15)
        ));
        JScrollPane accountScroll = new JScrollPane(accountBalancesPanel);
        accountScroll.setPreferredSize(new Dimension(300, 180));
        accountScroll.setBorder(BorderFactory.createEmptyBorder());
        financialSummaryPanel.add(accountScroll, BorderLayout.CENTER);

        // Budget Summary Panel / Panneau de résumé budgétaire
        budgetSummaryPanel = new JPanel();
        budgetSummaryPanel.setLayout(new BoxLayout(budgetSummaryPanel, BoxLayout.Y_AXIS));
        budgetSummaryPanel.setBackground(PANEL_BACKGROUND_COLOR);
        budgetSummaryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)), REPORT_BUDGET_SUMMARY_TITLE,
                        javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, SUBHEADER_FONT, TEXT_COLOR_DARK),
                new EmptyBorder(15, 15, 15, 15)
        ));
        JScrollPane budgetScroll = new JScrollPane(budgetSummaryPanel);
        budgetScroll.setPreferredSize(new Dimension(300, 180));
        budgetScroll.setBorder(BorderFactory.createEmptyBorder());
        financialSummaryPanel.add(budgetScroll, BorderLayout.SOUTH);

        // Button to manage budgets / Bouton pour gérer les budgets
        JButton manageBudgetsButton = new JButton(BUTTON_MANAGE_BUDGETS);
        manageBudgetsButton.setFont(LABEL_FONT);
        manageBudgetsButton.setForeground(Color.WHITE);
        manageBudgetsButton.setFocusPainted(false);
        manageBudgetsButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        manageBudgetsButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyHoverEffect(manageBudgetsButton, SECONDARY_COLOR, SECONDARY_COLOR.darker());
        JPanel budgetButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        budgetButtonPanel.setBackground(BACKGROUND_COLOR);
        budgetButtonPanel.add(manageBudgetsButton);
        financialSummaryPanel.add(budgetButtonPanel, BorderLayout.SOUTH);


        mainReportContent.add(financialSummaryPanel);

        // Right side: Pie Chart for spending by category / Côté droit: Graphique circulaire des dépenses par catégorie
        pieChartPanel = new PieChartPanel(); // Initialize the member variable / Initialiser le champ membre
        pieChartPanel.setPreferredSize(new Dimension(400, 400));
        pieChartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)), REPORT_EXPENSE_PIE_CHART_TITLE,
                        javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, SUBHEADER_FONT, TEXT_COLOR_DARK),
                new EmptyBorder(15, 15, 15, 15)
        ));
        mainReportContent.add(pieChartPanel);

        panel.add(mainReportContent, BorderLayout.CENTER);

        manageBudgetsButton.addActionListener(this::showBudgetManagementDialog);

        return panel;
    }

    /**
     * Displays a dialog for managing category budgets.
     *
     * <p>Affiche une boîte de dialogue pour gérer les budgets par catégorie.</p>
     */
    private void showBudgetManagementDialog(ActionEvent e) { // Added ActionEvent parameter
        // Collect all unique categories from transactions and default budgets
        // Recueillir toutes les catégories uniques des transactions et des budgets par défaut
        Set<TransactionCategory> allCategories = budgets.keySet().stream().collect(Collectors.toSet());
        transactions.stream()
                .map(Transaction::getCategory)
                .forEach(allCategories::add);

        allCategories.remove(TransactionCategory.SALAIRE); // Salary is not budgetable / Le salaire n'est pas budgétisable

        JPanel budgetDialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        List<JTextField> budgetFields = new ArrayList<>();
        List<TransactionCategory> categoriesToBudget = new ArrayList<>(allCategories);
        categoriesToBudget.sort(Comparator.comparing(Enum::toString)); // Sort for consistent order / Trier pour un ordre cohérent

        int row = 0;
        for (TransactionCategory category : categoriesToBudget) {
            JLabel categoryLabel = new JLabel(category.toString() + " Budget:");
            categoryLabel.setFont(LABEL_FONT);
            gbc.gridx = 0;
            gbc.gridy = row;
            budgetDialogPanel.add(categoryLabel, gbc);

            JTextField budgetField = new JTextField(formatAmountNoCurrency(budgets.getOrDefault(category, 0.0)), 15);
            budgetField.setFont(DATA_FONT);
            budgetFields.add(budgetField);
            gbc.gridx = 1;
            gbc.gridy = row;
            budgetDialogPanel.add(budgetField, gbc);
            row++;
        }

        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(budgetDialogPanel), BUTTON_MANAGE_BUDGETS,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                for (int i = 0; i < categoriesToBudget.size(); i++) {
                    TransactionCategory category = categoriesToBudget.get(i);
                    String budgetText = budgetFields.get(i).getText().trim();
                    double newBudget = Double.parseDouble(budgetText);
                    if (newBudget < 0) {
                        JOptionPane.showMessageDialog(this, MSG_WARNING_BUDGET_POSITIVE, "Erreur de saisie", JOptionPane.WARNING_MESSAGE);
                        return; // Stop processing and let user correct / Arrêter le traitement et laisser l'utilisateur corriger
                    }
                    budgets.put(category, newBudget);
                }
                updateUI(null); // Call updateUI with a null ActionEvent
                JOptionPane.showMessageDialog(this, MSG_SUCCESS_BUDGET_UPDATED, "Succès", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, MSG_ERROR_INVALID_BUDGET_AMOUNT, "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Calculates the global balance across all accounts.
     *
     * <p>Calcule le solde global de tous les comptes.</p>
     *
     * @return The total global balance. / Le solde global total.
     */
    private double getGlobalBalance() {
        return accountBalances.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Updates the balance of a specific account based on a transaction.
     *
     * <p>Met à jour le solde d'un compte spécifique en fonction d'une transaction.</p>
     *
     * @param account The name of the account. / Le nom du compte.
     * @param type The type of transaction (DEBIT or CREDIT). / Le type de transaction (DÉBIT ou CRÉDIT).
     * @param amount The amount of the transaction. / Le montant de la transaction.
     */
    private void updateAccountBalance(String account, TransactionType type, double amount) {
        double currentBalance = accountBalances.getOrDefault(account, 0.0);
        if (type == TransactionType.DEBIT) {
            currentBalance -= amount;
        } else { // type == TransactionType.CREDIT
            currentBalance += amount;
        }
        accountBalances.put(account, currentBalance);
    }

    /**
     * Clears the input fields for adding new transactions.
     *
     * <p>Efface les champs de saisie pour l'ajout de nouvelles transactions.</p>
     */
    private void clearTransactionFields() {
        descriptionField.setText("");
        montantField.setText("");
        typeComboBox.setSelectedIndex(0);
        categoryComboBox.setSelectedIndex(0);
        accountComboBox.setSelectedIndex(0);
    }

    /**
     * Updates the entire User Interface, including the transaction table, global balance label,
     * account balances panel, budget summary, and pie chart.
     *
     * <p>Met à jour l'intégralité de l'interface utilisateur, y compris le tableau des transactions,
     * l'étiquette du solde global, le panneau des soldes de compte, le résumé budgétaire et le graphique circulaire.</p>
     */
    private void updateUI(ActionEvent e) { // Added ActionEvent parameter
        updateTableDisplay();
        updateGlobalBalanceLabel();
        updateAccountBalancesDisplay();
        updateReportSummary();
        updateBudgetSummaryUI();
        updatePieChartData();
    }

    /**
     * Updates the transaction table display based on current filters and search text.
     *
     * <p>Met à jour l'affichage du tableau des transactions en fonction des filtres
     * et du texte de recherche actuels.</p>
     */
    private void updateTableDisplay() {
        tableModel.setRowCount(0); // Clear existing rows / Effacer les lignes existantes

        String selectedTypeFilter = (String) filterTypeComboBox.getSelectedItem();
        String selectedCategoryFilter = (String) filterCategoryComboBox.getSelectedItem();
        String searchText = searchField.getText().trim().toLowerCase();

        // Filter transactions / Filtrer les transactions
        List<Transaction> filteredTransactions = transactions.stream()
                .filter(t -> "Tous Types".equals(selectedTypeFilter) || t.getType().toString().equals(selectedTypeFilter))
                .filter(t -> "Toutes Catégories".equals(selectedCategoryFilter) || t.getCategory().toString().toLowerCase().contains(selectedCategoryFilter.toLowerCase()))
                .filter(t -> searchText.isEmpty() ||
                             t.getDescription().toLowerCase().contains(searchText) ||
                             t.getAccount().toLowerCase().contains(searchText) ||
                             t.getCategory().toString().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Transaction t : filteredTransactions) {
            tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getDate().format(formatter),
                    t.getAccount(),
                    t.getType().toString(), // Use toString() for display / Utiliser toString() pour l'affichage
                    t.getCategory().toString(), // Use toString() for display / Utiliser toString() pour l'affichage
                    t.getDescription(),
                    formatCurrency(t.getMontant())
            });
        }
    }

    /**
     * Updates the global balance label's text and color.
     *
     * <p>Met à jour le texte et la couleur de l'étiquette du solde global.</p>
     */
    private void updateGlobalBalanceLabel() {
        double globalBalance = getGlobalBalance();
        soldeGlobalLabel.setText("Solde Global: " + formatCurrency(globalBalance));
        if (globalBalance < 0) {
            soldeGlobalLabel.setForeground(ACCENT_COLOR_NEGATIVE);
        } else {
            soldeGlobalLabel.setForeground(Color.WHITE);
        }
    }

    /**
     * Updates the display of individual account balances.
     *
     * <p>Met à jour l'affichage des soldes de compte individuels.</p>
     */
    private void updateAccountBalancesDisplay() {
        accountBalancesPanel.removeAll();
        // Sort accounts alphabetically for consistent display / Trier les comptes par ordre alphabétique pour un affichage cohérent
        accountBalances.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    JLabel accountLabel = new JLabel(entry.getKey() + ": " + formatCurrency(entry.getValue()));
                    accountLabel.setFont(DATA_FONT.deriveFont(Font.BOLD, 16));
                    accountLabel.setBorder(new EmptyBorder(7, 0, 7, 0));
                    if (entry.getValue() < 0) {
                        accountLabel.setForeground(ACCENT_COLOR_NEGATIVE);
                    } else {
                        accountLabel.setForeground(TEXT_COLOR_DARK);
                    }
                    accountBalancesPanel.add(accountLabel);
                });
        accountBalancesPanel.revalidate();
        accountBalancesPanel.repaint();
    }

    /**
     * Updates the total debit and credit labels in the reports section.
     *
     * <p>Met à jour les étiquettes de débit et de crédit totaux dans la section des rapports.</p>
     */
    private void updateReportSummary() {
        double totalDebit = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .mapToDouble(Transaction::getMontant)
                .sum();
        double totalCredit = transactions.stream()
                .filter(t -> t.getType() == TransactionType.CREDIT)
                .mapToDouble(Transaction::getMontant)
                .sum();

        totalDebitLabel.setText(formatCurrency(totalDebit));
        totalCreditLabel.setText(formatCurrency(totalCredit));
    }

    /**
     * Updates the budget summary display, showing spent vs. budgeted amounts for each category.
     * Highlights categories where the budget has been exceeded.
     *
     * <p>Met à jour l'affichage du résumé budgétaire, affichant les montants dépensés par rapport
     * aux montants budgétisés pour chaque catégorie. Met en évidence les catégories
     * où le budget a été dépassé.</p>
     */
    private void updateBudgetSummaryUI() {
        budgetSummaryPanel.removeAll();
        // Calculate current month's expenses by category / Calculer les dépenses du mois en cours par catégorie
        Map<TransactionCategory, Double> currentMonthExpensesByCategory = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .filter(t -> t.getDate().getYear() == LocalDate.now().getYear() && t.getDate().getMonth() == LocalDate.now().getMonth())
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getMontant)));

        // Sort categories by name / Trier les catégories par nom
        List<TransactionCategory> sortedCategories = new ArrayList<>(budgets.keySet());
        sortedCategories.sort(Comparator.comparing(Enum::toString));

        for (TransactionCategory category : sortedCategories) {
            double budgeted = budgets.getOrDefault(category, 0.0);
            double spent = currentMonthExpensesByCategory.getOrDefault(category, 0.0);

            JLabel budgetLabel = new JLabel(category.toString() + ": " + formatCurrency(spent) + " / " + formatCurrency(budgeted));
            budgetLabel.setFont(DATA_FONT.deriveFont(Font.BOLD, 14));
            budgetLabel.setBorder(new EmptyBorder(5, 0, 5, 0));

            // Color coding for budget status / Codage couleur pour l'état du budget
            if (budgeted > 0 && spent > budgeted) {
                budgetLabel.setForeground(ACCENT_COLOR_NEGATIVE); // Exceeded budget / Budget dépassé
            } else if (budgeted > 0 && spent <= budgeted * 0.8) {
                budgetLabel.setForeground(ACCENT_COLOR_POSITIVE); // Well within budget / Bien en deçà du budget
            } else {
                budgetLabel.setForeground(TEXT_COLOR_DARK); // Default / Par défaut
            }
            budgetSummaryPanel.add(budgetLabel);
        }
        budgetSummaryPanel.revalidate();
        budgetSummaryPanel.repaint();
    }

    /**
     * Updates the data displayed in the pie chart for category expenses.
     *
     * <p>Met à jour les données affichées dans le graphique circulaire pour les dépenses par catégorie.</p>
     */
    private void updatePieChartData() {
        Map<String, Double> categoryExpenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .collect(Collectors.groupingBy(t -> t.getCategory().toString(), Collectors.summingDouble(Transaction::getMontant)));

        if (pieChartPanel != null) { // Ensure pieChartPanel is initialized / S'assurer que pieChartPanel est initialisé
            pieChartPanel.updateData(categoryExpenses);
        }
    }

    /**
     * Edits a selected transaction based on user input from a dialog.
     *
     * <p>Modifie une transaction sélectionnée en fonction de l'entrée de l'utilisateur
     * à partir d'une boîte de dialogue.</p>
     */
    private void editTransaction(ActionEvent e) { // Added ActionEvent parameter
        int selectedRow = transactionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, MSG_WARNING_NO_SELECTION, "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long transactionId = (Long) tableModel.getValueAt(selectedRow, 0);
        Transaction transactionToEdit = transactions.stream()
                .filter(t -> t.getId() == transactionId)
                .findFirst()
                .orElse(null);

        if (transactionToEdit == null) {
            JOptionPane.showMessageDialog(this, MSG_ERROR_TRANSACTION_NOT_FOUND, "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Initialize dialog fields with current transaction data / Initialiser les champs de dialogue avec les données de transaction actuelles
        JTextField editDescriptionField = new JTextField(transactionToEdit.getDescription());
        JTextField editMontantField = new JTextField(formatAmountNoCurrency(transactionToEdit.getMontant()));
        JComboBox<TransactionType> editTypeComboBox = new JComboBox<>(TransactionType.values());
        editTypeComboBox.setSelectedItem(transactionToEdit.getType());
        JComboBox<TransactionCategory> editCategoryComboBox = new JComboBox<>(TransactionCategory.values());
        editCategoryComboBox.setSelectedItem(transactionToEdit.getCategory());
        JComboBox<String> editAccountComboBox = new JComboBox<>(accountBalances.keySet().toArray(new String[0]));
        editAccountComboBox.setSelectedItem(transactionToEdit.getAccount());

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8)); // Use GridLayout for better alignment / Utiliser GridLayout pour un meilleur alignement
        panel.add(new JLabel(LABEL_ACCOUNT));
        panel.add(editAccountComboBox);
        panel.add(new JLabel(LABEL_TYPE));
        panel.add(editTypeComboBox);
        panel.add(new JLabel(LABEL_CATEGORY));
        panel.add(editCategoryComboBox);
        panel.add(new JLabel(LABEL_DESCRIPTION));
        panel.add(editDescriptionField);
        panel.add(new JLabel(LABEL_AMOUNT));
        panel.add(editMontantField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Modifier Transaction",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String newAccount = (String) editAccountComboBox.getSelectedItem();
                TransactionType newType = (TransactionType) editTypeComboBox.getSelectedItem();
                TransactionCategory newCategory = (TransactionCategory) editCategoryComboBox.getSelectedItem();
                String newDescription = editDescriptionField.getText().trim();
                double newMontant = Double.parseDouble(editMontantField.getText().trim());

                if (newDescription.isEmpty() || newMontant <= 0) {
                    JOptionPane.showMessageDialog(this, MSG_WARNING_EMPTY_FIELDS + " " + MSG_WARNING_POSITIVE_AMOUNT, "Erreur de saisie", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Revert the old transaction's impact / Annuler l'impact de l'ancienne transaction
                double oldMontant = transactionToEdit.getMontant();
                TransactionType oldType = transactionToEdit.getType();
                String oldAccount = transactionToEdit.getAccount();
                updateAccountBalance(oldAccount, oldType.reverse(), oldMontant); // Reverse the old transaction / Inverser l'ancienne transaction

                // Update the transaction object / Mettre à jour l'objet transaction
                transactionToEdit.setAccount(newAccount);
                transactionToEdit.setType(newType);
                transactionToEdit.setCategory(newCategory);
                transactionToEdit.setDescription(newDescription);
                transactionToEdit.setMontant(newMontant);

                // Apply the new transaction's impact / Appliquer l'impact de la nouvelle transaction
                updateAccountBalance(newAccount, newType, newMontant);

                updateUI(null); // Call updateUI with a null ActionEvent
                JOptionPane.showMessageDialog(this, MSG_SUCCESS_TRANSACTION_MODIFIED, "Succès", JOptionPane.INFORMATION_MESSAGE);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, MSG_ERROR_INVALID_AMOUNT, "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Deletes the selected transaction from the table and updates account balances.
     *
     * <p>Supprime la transaction sélectionnée du tableau et met à jour les soldes de compte.</p>
     */
    private void deleteSelectedTransaction(ActionEvent e) { // Added ActionEvent parameter
        int selectedRow = transactionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, MSG_WARNING_NO_SELECTION, "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, MSG_CONFIRM_DELETE, "Confirmer la suppression",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            long transactionId = (Long) tableModel.getValueAt(selectedRow, 0);
            // Use iterator for safe removal during iteration or stream filter if not iterating
            // Utiliser un itérateur pour une suppression sécurisée pendant l'itération ou un filtre de flux si non-itération
            Transaction transactionToDelete = null;
            for (int i = 0; i < transactions.size(); i++) {
                if (transactions.get(i).getId() == transactionId) {
                    transactionToDelete = transactions.get(i);
                    transactions.remove(i); // Remove directly / Supprimer directement
                    break;
                }
            }

            if (transactionToDelete != null) {
                double amount = transactionToDelete.getMontant();
                TransactionType type = transactionToDelete.getType();
                String account = transactionToDelete.getAccount();

                // Revert the transaction's impact on the balance / Annuler l'impact de la transaction sur le solde
                updateAccountBalance(account, type.reverse(), amount); // Reverse the type / Inverser le type

                updateUI(null); // Call updateUI with a null ActionEvent
                JOptionPane.showMessageDialog(this, MSG_SUCCESS_TRANSACTION_DELETED, "Succès", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Allows the user to export current transactions to a CSV file.
     *
     * <p>Permet à l'utilisateur d'exporter les transactions actuelles vers un fichier CSV.</p>
     */
    private void exportTransactionsToCSV(ActionEvent e) { // Added ActionEvent parameter
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Enregistrer les transactions au format CSV");
        String userHome = System.getProperty("user.home");
        File documentsDir = new File(userHome, "Documents");
        if (documentsDir.exists() && documentsDir.isDirectory()) {
            fileChooser.setCurrentDirectory(documentsDir);
        } else {
            fileChooser.setCurrentDirectory(new File(userHome));
        }

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
            }
            saveTransactionsToCSV(filePath, true); // True to indicate user-initiated export / Vrai pour indiquer une exportation initiée par l'utilisateur
        }
    }

    /**
     * Saves transaction data to a specified CSV file.
     *
     * <p>Sauvegarde les données de transaction dans un fichier CSV spécifié.</p>
     *
     * @param filePath The path to the CSV file. / Le chemin du fichier CSV.
     * @param isUserExport True if this is a user-initiated export, false for automatic persistence.
     * / Vrai si c'est une exportation initiée par l'utilisateur, faux pour la persistance automatique.
     */
    private void saveTransactionsToCSV(String filePath, boolean isUserExport) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write CSV header / Écrire l'en-tête CSV
            writer.append("\"ID\",\"Date\",\"Compte\",\"Type\",\"Catégorie\",\"Description\",\"Montant\"");
            writer.newLine();

            // Write CSV data / Écrire les données CSV
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Transaction t : transactions) {
                writer.append("\"").append(String.valueOf(t.getId())).append("\"").append(",");
                writer.append("\"").append(t.getDate().format(formatter)).append("\"").append(",");
                writer.append("\"").append(escapeCsv(t.getAccount())).append("\"").append(",");
                writer.append("\"").append(escapeCsv(t.getType().toString())).append("\"").append(",");
                writer.append("\"").append(escapeCsv(t.getCategory().toString())).append("\"").append(",");
                writer.append("\"").append(escapeCsv(t.getDescription())).append("\"").append(",");
                writer.append("\"").append(String.valueOf(t.getMontant())).append("\"");
                writer.newLine();
            }
            if (isUserExport) {
                JOptionPane.showMessageDialog(this, MSG_SUCCESS_EXPORT + filePath, "Exportation réussie", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            if (isUserExport) {
                JOptionPane.showMessageDialog(this, MSG_ERROR_EXPORT + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            } else {
                System.err.println("Erreur lors de la sauvegarde automatique des transactions (CSV): " + ex.getMessage());
            }
            ex.printStackTrace();
        }
    }

    /**
     * Escapes a string for CSV output by enclosing it in double quotes and escaping existing double quotes.
     * Handles null values by returning an empty string.
     *
     * <p>Échappe une chaîne pour la sortie CSV en l'entourant de guillemets doubles
     * et en échappant les guillemets doubles existants. Gère les valeurs nulles
     * en retournant une chaîne vide.</p>
     *
     * @param value The string to escape. / La chaîne à échapper.
     * @return The escaped string. / La chaîne échappée.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    /**
     * Orchestrates saving all application data (transactions, account balances, and budgets)
     * using object serialization. Also creates backup files for robustness.
     *
     * <p>Orchestre la sauvegarde de toutes les données de l'application (transactions,
     * soldes de compte et budgets) en utilisant la sérialisation d'objets.
     * Crée également des fichiers de sauvegarde pour la robustesse.</p>
     */
    private void saveData() {
        // Backup existing files before saving new data
        // Sauvegarder les fichiers existants avant d'enregistrer de nouvelles données
        copyFile(transactionsFileSer, transactionsFileSerBackup);
        copyFile(balancesAndBudgetsFileSer, balancesAndBudgetsFileSerBackup);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(transactionsFileSer))) {
            oos.writeObject(transactions);
            System.out.println("Transactions sauvegardées via sérialisation.");
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde des transactions: " + e.getMessage());
            e.printStackTrace();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(balancesAndBudgetsFileSer))) {
            oos.writeObject(accountBalances);
            oos.writeObject(budgets); // Save budgets as well / Sauvegarder également les budgets
            System.out.println("Soldes de compte et budgets sauvegardés via sérialisation.");
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde des soldes de compte et budgets: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Données sauvegardées.");
    }

    /**
     * Orchestrates loading all application data (transactions, account balances, and budgets)
     * using object deserialization. Attempts to load from backup files if primary files are not found or corrupted.
     *
     * <p>Orchestre le chargement de toutes les données de l'application (transactions,
     * soldes de compte et budgets) en utilisant la désérialisation d'objets.
     * Tente de charger à partir des fichiers de sauvegarde si les fichiers principaux
     * sont introuvables ou corrompus.</p>
     */
    @SuppressWarnings("unchecked") // Suppress unchecked cast warnings for deserialization
    private void loadData() {
        // Load account balances and budgets first
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(balancesAndBudgetsFileSer))) {
            accountBalances = (Map<String, Double>) ois.readObject();
            // Attempt to read budgets, allowing for older String-based keys
            Object readBudgets = ois.readObject();
            if (readBudgets instanceof Map) {
                Map<?, Double> tempMap = (Map<?, Double>) readBudgets;
                budgets = new HashMap<>(); // Reinitialize to a fresh, correctly typed map
                for (Map.Entry<?, Double> entry : tempMap.entrySet()) {
                    if (entry.getKey() instanceof String) {
                        try {
                            // Convert old String key to new enum key
                            budgets.put(TransactionCategory.fromString((String) entry.getKey()), entry.getValue());
                        } catch (IllegalArgumentException e) {
                            System.err.println("Warning: Could not convert old budget category string '" + entry.getKey() + "' to enum. Assigning to 'Autre'.");
                            budgets.put(TransactionCategory.AUTRE, entry.getValue());
                        }
                    } else if (entry.getKey() instanceof TransactionCategory) {
                        // Key is already the correct enum type
                        budgets.put((TransactionCategory) entry.getKey(), entry.getValue());
                    } else {
                        System.err.println("Warning: Unexpected type for budget category key: " + entry.getKey().getClass().getName() + ". Skipping entry.");
                    }
                }
            } else {
                System.err.println("Warning: Deserialized budget object is not a Map. Initializing budgets to default values.");
                // Fallback to default budgets if deserialized object is not a map
                Arrays.stream(TransactionCategory.values())
                        .filter(cat -> cat != TransactionCategory.SALAIRE)
                        .forEach(cat -> budgets.putIfAbsent(cat, 0.0));
            }
            System.out.println("Soldes de compte et budgets chargés via sérialisation.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur lors du chargement des soldes de compte/budgets ou fichier non trouvé. Tentative de chargement depuis la sauvegarde. " + e.getMessage());
            // Attempt to load from backup if main file fails
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(balancesAndBudgetsFileSerBackup))) {
                accountBalances = (Map<String, Double>) ois.readObject();
                Object readBudgets = ois.readObject(); // Try reading from backup
                if (readBudgets instanceof Map) {
                    Map<?, Double> tempMap = (Map<?, Double>) readBudgets;
                    budgets = new HashMap<>();
                    for (Map.Entry<?, Double> entry : tempMap.entrySet()) {
                        if (entry.getKey() instanceof String) {
                            try {
                                budgets.put(TransactionCategory.fromString((String) entry.getKey()), entry.getValue());
                            } catch (IllegalArgumentException e2) {
                                System.err.println("Warning: Could not convert old backup budget category string '" + entry.getKey() + "' to enum. Assigning to 'Autre'.");
                                budgets.put(TransactionCategory.AUTRE, entry.getValue());
                            }
                        } else if (entry.getKey() instanceof TransactionCategory) {
                            budgets.put((TransactionCategory) entry.getKey(), entry.getValue());
                        } else {
                            System.err.println("Warning: Unexpected type for backup budget category key: " + entry.getKey().getClass().getName() + ". Skipping entry.");
                        }
                    }
                } else {
                    System.err.println("Warning: Deserialized backup budget object is not a Map. Initializing budgets to default values.");
                    Arrays.stream(TransactionCategory.values())
                            .filter(cat -> cat != TransactionCategory.SALAIRE)
                            .forEach(cat -> budgets.putIfAbsent(cat, 0.0));
                }
                System.out.println("Soldes de compte et budgets chargés depuis la sauvegarde.");
            } catch (IOException | ClassNotFoundException backupE) {
                System.err.println("Échec du chargement de la sauvegarde des soldes/budgets. Utilisation des valeurs par défaut. " + backupE.getMessage());
                // Ensure default accounts and budgets are present if loading fails
                accountBalances.putIfAbsent(DEFAULT_ACCOUNT_CASH, 0.0);
                accountBalances.putIfAbsent(DEFAULT_ACCOUNT_BANK, 0.0);
                accountBalances.putIfAbsent(DEFAULT_ACCOUNT_SAVINGS, 0.0);
                Arrays.stream(TransactionCategory.values())
                      .filter(cat -> cat != TransactionCategory.SALAIRE)
                      .forEach(cat -> budgets.putIfAbsent(cat, 0.0));
            }
        }

        // Load transactions
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(transactionsFileSer))) {
            transactions = (List<Transaction>) ois.readObject();
            // Update nextId after loading all transactions / Mettre à jour nextId après le chargement de toutes les transactions
            long maxId = transactions.stream().mapToLong(Transaction::getId).max().orElse(0L);
            Transaction.nextId = maxId + 1;
            System.out.println("Transactions chargées via sérialisation. Prochain ID: " + Transaction.nextId);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur lors du chargement des transactions ou fichier non trouvé. Démarrage avec des transactions vides. " + e.getMessage());
            // Attempt to load from backup if main file fails / Tenter de charger à partir de la sauvegarde si le fichier principal échoue
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(transactionsFileSerBackup))) {
                transactions = (List<Transaction>) ois.readObject();
                long maxId = transactions.stream().mapToLong(Transaction::getId).max().orElse(0L);
                Transaction.nextId = maxId + 1;
                System.out.println("Transactions chargées depuis la sauvegarde.");
            } catch (IOException | ClassNotFoundException backupE) {
                System.err.println("Échec du chargement de la sauvegarde des transactions. Démarrage avec des transactions vides. " + backupE.getMessage());
                transactions = new ArrayList<>(); // Ensure transactions list is not null / S'assurer que la liste des transactions n'est pas nulle
                Transaction.nextId = 1; // Reset ID if no data loaded / Réinitialiser l'ID si aucune donnée n'est chargée
            }
        }
    }

    /**
     * Copies a file from sourcePath to destPath. Used for creating backup files.
     *
     * <p>Copie un fichier de sourcePath vers destPath. Utilisé pour créer des fichiers de sauvegarde.</p>
     *
     * @param sourcePath The path of the source file. / Le chemin du fichier source.
     * @param destPath The path of the destination file. / Le chemin du fichier de destination.
     */
    private void copyFile(String sourcePath, String destPath) {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        if (sourceFile.exists()) {
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                System.out.println("Fichier de sauvegarde créé pour: " + sourcePath);
            } catch (IOException e) {
                System.err.println("Erreur lors de la création de la sauvegarde pour " + sourcePath + ": " + e.getMessage());
            }
        }
    }

    /**
     * Formats a double amount into a currency string specific to France (Euro).
     *
     * <p>Formate un montant double en une chaîne de caractères monétaire spécifique à la France (Euro).</p>
     *
     * @param amount The double amount to format. / Le montant double à formater.
     * @return A formatted currency string. / Une chaîne de caractères monétaire formatée.
     */
    private String formatCurrency(double amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        return currencyFormat.format(amount);
    }

    /**
     * Formats a double amount into a string without currency symbol, with two decimal places.
     * Useful for displaying amounts in input fields or budgets.
     *
     * <p>Formate un montant double en une chaîne sans symbole monétaire, avec deux décimales.
     * Utile pour afficher les montants dans les champs de saisie ou les budgets.</p>
     *
     * @param doubleAmount The double amount to format. / Le montant double à formater.
     * @return A formatted amount string. / Une chaîne de caractères de montant formatée.
     */
    private String formatAmountNoCurrency(double doubleAmount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.FRANCE);
        nf.setGroupingUsed(false); // Do not use grouping separators / Ne pas utiliser de séparateurs de regroupement
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
        return nf.format(doubleAmount);
    }

    /**
     * Main method to start the BeyahCompta application.
     * Ensures that the UI is created and updated on the Event Dispatch Thread (EDT).
     *
     * <p>Méthode principale pour démarrer l'application BeyahCompta.
     * S'assure que l'interface utilisateur est créée et mise à jour sur le
     * Event Dispatch Thread (EDT).</p>
     *
     * @param args Command line arguments (not used). / Arguments de ligne de commande (non utilisés).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BeyahCompta::new); // Use lambda for concise instance creation / Utiliser une lambda pour la création d'instance concise
    }

    /**
     * Enum representing the type of a financial transaction (Debit or Credit).
     *
     * <p>Énumération représentant le type d'une transaction financière (Débit ou Crédit).</p>
     */
    public enum TransactionType { // Removed implements Serializable
        DEBIT("Débit"),
        CREDIT("Crédit");

        private static final long serialVersionUID = 1L; // Still good practice to keep
        private final String displayValue;

        TransactionType(String displayValue) {
            this.displayValue = displayValue;
        }

        @Override
        public String toString() {
            return displayValue;
        }

        /**
         * Returns the corresponding TransactionType enum from its display string.
         *
         * <p>Retourne l'énumération TransactionType correspondante à partir de sa chaîne d'affichage.</p>
         *
         * @param text The display string (e.g., "Débit", "Crédit"). / La chaîne d'affichage (par exemple, "Débit", "Crédit").
         * @return The TransactionType enum. / L'énumération TransactionType.
         * @throws IllegalArgumentException If no matching type is found. / Si aucun type correspondant n'est trouvé.
         */
        public static TransactionType fromString(String text) {
            for (TransactionType b : TransactionType.values()) {
                if (b.displayValue.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }

        /**
         * Returns the reverse type of the transaction (Debit for Credit, Credit for Debit).
         * Useful for reverting account balance changes.
         *
         * <p>Retourne le type inverse de la transaction (Débit pour Crédit, Crédit pour Débit).
         * Utile pour annuler les changements de solde de compte.</p>
         *
         * @return The reversed TransactionType. / Le TransactionType inversé.
         */
        public TransactionType reverse() {
            return this == DEBIT ? CREDIT : DEBIT;
        }
    }

    /**
     * Enum representing various categories for financial transactions.
     *
     * <p>Énumération représentant diverses catégories pour les transactions financières.</p>
     */
    public enum TransactionCategory { // Removed implements Serializable
        GENERAL(CATEGORY_GENERAL),
        NOURRITURE(CATEGORY_FOOD),
        TRANSPORT(CATEGORY_TRANSPORT),
        LOISIRS(CATEGORY_LEISURE),
        SALAIRE(CATEGORY_SALARY),
        AUTRE(CATEGORY_OTHER);

        private static final long serialVersionUID = 1L; // Still good practice to keep
        private final String displayValue;

        TransactionCategory(String displayValue) {
            this.displayValue = displayValue;
        }

        @Override
        public String toString() {
            return displayValue;
        }

        /**
         * Returns the corresponding TransactionCategory enum from its display string.
         *
         * <p>Retourne l'énumération TransactionCategory correspondante à partir de sa chaîne d'affichage.</p>
         *
         * @param text The display string. / La chaîne d'affichage.
         * @return The TransactionCategory enum. / L'énumération TransactionCategory.
         * @throws IllegalArgumentException If no matching category is found. / Si aucune catégorie correspondante n'est trouvée.
         */
        public static TransactionCategory fromString(String text) {
            for (TransactionCategory b : TransactionCategory.values()) {
                if (b.displayValue.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }

    /**
     * Inner class for custom Pie Chart rendering of expenses by category.
     *
     * <p>Classe interne pour le rendu personnalisé d'un graphique circulaire des dépenses par catégorie.</p>
     */
    class PieChartPanel extends JPanel {
        private Map<String, Double> data;
        private List<Color> colors;

        /**
         * Constructs a PieChartPanel. Initializes data and a set of predefined colors.
         *
         * <p>Construit un PieChartPanel. Initialise les données et un ensemble de couleurs prédéfinies.</p>
         */
        public PieChartPanel() {
            this.data = new HashMap<>();
            // Define a set of distinct colors for the pie slices / Définir un ensemble de couleurs distinctes pour les tranches du graphique
            this.colors = new ArrayList<>(Arrays.asList(
                    new Color(255, 99, 132), // Red / Rouge
                    new Color(54, 162, 235), // Blue / Bleu
                    new Color(255, 206, 86), // Yellow / Jaune
                    new Color(75, 192, 192), // Green / Vert
                    new Color(153, 102, 255), // Purple / Violet
                    new Color(255, 159, 64), // Orange / Orange
                    new Color(199, 199, 199), // Grey / Gris
                    new Color(83, 109, 254)  // Indigo
            ));
        }

        /**
         * Updates the data to be displayed in the pie chart and repaints the component.
         *
         * <p>Met à jour les données à afficher dans le graphique circulaire et redessine le composant.</p>
         *
         * @param newData A map where keys are category names (String) and values are total expenses (Double).
         * / Une carte où les clés sont les noms de catégorie (String) et les valeurs sont les dépenses totales (Double).
         */
        public void updateData(Map<String, Double> newData) {
            this.data = newData;
            repaint(); // Redraw the chart with new data / Redessiner le graphique avec de nouvelles données
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int diameter = Math.min(width, height) - 40; // Subtract padding / Soustraire le rembourrage
            int x = (width - diameter) / 2;
            int y = (height - diameter) / 2;

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();

            if (total == 0) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillOval(x, y, diameter, diameter);
                g2d.setColor(TEXT_COLOR_DARK);
                g2d.setFont(LABEL_FONT);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(MSG_NO_EXPENSE_DATA);
                int textHeight = fm.getHeight();
                g2d.drawString(MSG_NO_EXPENSE_DATA, (width - textWidth) / 2, height / 2 + textHeight / 4);
                return;
            }

            double startAngle = 0;
            int colorIndex = 0;
            List<Map.Entry<String, Double>> sortedEntries = data.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // Sort by value for consistent slice order / Trier par valeur pour un ordre de tranches cohérent
                    .collect(Collectors.toList());

            for (Map.Entry<String, Double> entry : sortedEntries) {
                double value = entry.getValue();
                double angle = (value / total) * 360;

                g2d.setColor(colors.get(colorIndex % colors.size()));
                g2d.fillArc(x, y, diameter, diameter, (int) startAngle, (int) angle);

                // Draw labels (Category and Percentage) / Dessiner les étiquettes (Catégorie et Pourcentage)
                double midAngle = startAngle + angle / 2;
                double radians = Math.toRadians(midAngle);
                int labelX = (int) (x + diameter / 2 + (diameter / 2 + 10) * Math.cos(radians));
                int labelY = (int) (y + diameter / 2 + (diameter / 2 + 10) * Math.sin(radians));

                String labelText = String.format("%s (%.1f%%)", entry.getKey(), (value / total) * 100);
                g2d.setColor(TEXT_COLOR_DARK);
                g2d.setFont(DATA_FONT);

                // Adjust label position for better readability / Ajuster la position de l'étiquette pour une meilleure lisibilité
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(labelText);
                int textHeight = fm.getHeight();

                if (Math.cos(radians) > 0) { // Right half / Moitié droite
                    // Align text to the right for right side labels / Aligner le texte à droite pour les étiquettes du côté droit
                    labelX = (int) (x + diameter / 2 + (diameter / 2 + 10) * Math.cos(radians));
                } else { // Left half / Moitié gauche
                    // Align text to the left for left side labels / Aligner le texte à gauche pour les étiquettes du côté gauche
                    labelX = (int) (x + diameter / 2 + (diameter / 2 + 10) * Math.cos(radians) - textWidth);
                }

                if (Math.sin(radians) > 0) { // Bottom half / Moitié inférieure
                    labelY = (int) (y + diameter / 2 + (diameter / 2 + 10) * Math.sin(radians));
                } else { // Top half / Moitié supérieure
                    labelY = (int) (y + diameter / 2 + (diameter / 2 + 10) * Math.sin(radians) + textHeight);
                }

                g2d.drawString(labelText, labelX, labelY);

                startAngle += angle;
                colorIndex++;
            }
        }
    }
}

/**
 * Represents a financial transaction within the BeyahCompta application.
 * This class is Serializable to allow for persistence.
 *
 * <p>Représente une transaction financière au sein de l'application BeyahCompta.
 * Cette classe est Sérialisable pour permettre la persistance.</p>
 */
class Transaction implements Serializable {
    // IMPORTANT: Reverted serialVersionUID to 1L to ensure backward compatibility with
    // previously serialized files that had String types for 'type' and 'category' fields.
    // The custom readObject method below handles the String to Enum conversion.
    private static final long serialVersionUID = 1L;

    /**
     * The next available ID for a new transaction.
     * This field is public static to ensure visibility and proper incrementation across instances.
     *
     * <p>Le prochain ID disponible pour une nouvelle transaction.
     * Ce champ est public statique pour assurer la visibilité et l'incrémentation correcte entre les instances.</p>
     */
    public static long nextId = 1;

    private long id;
    private LocalDate date;
    private String account;
    private BeyahCompta.TransactionType type; // Using enum / Utilisation de l'énumération
    private BeyahCompta.TransactionCategory category; // Using enum / Utilisation de l'énumération
    private String description;
    private double montant;

    /**
     * Constructor for creating a new transaction. Automatically assigns a unique ID and the current date.
     *
     * <p>Constructeur pour créer une nouvelle transaction. Assignée automatiquement un ID unique et la date actuelle.</p>
     *
     * @param account The account name associated with the transaction. / Le nom du compte associé à la transaction.
     * @param type The type of transaction (Debit or Credit). / Le type de transaction (Débit ou Crédit).
     * @param category The category of the transaction. / La catégorie de la transaction.
     * @param description A brief description of the transaction. / Une brève description de la transaction.
     * @param montant The amount of the transaction. / Le montant de la transaction.
     */
    public Transaction(String account, BeyahCompta.TransactionType type, BeyahCompta.TransactionCategory category, String description, double montant) {
        this.id = nextId++;
        this.date = LocalDate.now();
        this.account = account;
        this.type = type;
        this.category = category;
        this.description = description;
        this.montant = montant;
    }

    /**
     * Constructor for loading an existing transaction from persistent storage.
     * Allows setting the ID and date explicitly.
     *
     * <p>Constructeur pour charger une transaction existante à partir du stockage persistant.
     * Permet de définir explicitement l'ID et la date.</p>
     *
     * @param id The unique identifier of the transaction. / L'identifiant unique de la transaction.
     * @param date The date of the transaction. / La date de la transaction.
     * @param account The account name. / Le nom du compte.
     * @param type The type of transaction. / Le type de transaction.
     * @param category The category of the transaction. / La catégorie de la transaction.
     * @param description The description of the transaction. / La description de la transaction.
     * @param montant The amount of the transaction. / Le montant de la transaction.
     */
    public Transaction(long id, LocalDate date, String account, BeyahCompta.TransactionType type, BeyahCompta.TransactionCategory category, String description, double montant) {
        this.id = id;
        this.date = date;
        this.account = account;
        this.type = type;
        this.category = category;
        this.description = description;
        this.montant = montant;
    }

    // --- Getters ---
    public long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getAccount() { return account; }
    public BeyahCompta.TransactionType getType() { return type; } // Returns enum / Retourne l'énumération
    public BeyahCompta.TransactionCategory getCategory() { return category; } // Retourne l'énumération
    public String getDescription() { return description; }
    public double getMontant() { return montant; }

    // --- Setters ---
    public void setAccount(String account) { this.account = account; }
    public void setType(BeyahCompta.TransactionType type) { this.type = type; } // Accepts enum / Accepte l'énumération
    public void setCategory(BeyahCompta.TransactionCategory category) { this.category = category; } // Accepte l'énumération
    public void setDescription(String description) { this.description = description; }
    public void setMontant(double montant) { this.montant = montant; }

    /**
     * Custom deserialization method to handle backward compatibility.
     * It addresses the change of 'type' and 'category' fields from String to Enum.
     *
     * <p>Méthode de désérialisation personnalisée pour gérer la compatibilité ascendante.
     * Elle gère le changement des champs 'type' et 'category' de String à Enum.</p>
     *
     * @param s The ObjectInputStream from which to read the object.
     * @throws IOException If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object could not be found.
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();

        // Read primitive and unchanged object fields directly
        this.id = fields.get("id", 0L);
        this.date = (LocalDate) fields.get("date", null);
        this.account = (String) fields.get("account", null);
        this.description = (String) fields.get("description", null);
        this.montant = fields.get("montant", 0.0);

        // Handle 'type' field: it could be a String (old version) or TransactionType enum (new version)
        Object typeObj = fields.get("type", null);
        if (typeObj instanceof String) {
            try {
                this.type = BeyahCompta.TransactionType.fromString((String) typeObj);
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Could not convert old 'type' string '" + typeObj + "' to enum. Assigning DEBIT.");
                this.type = BeyahCompta.TransactionType.DEBIT; // Default to DEBIT if conversion fails
            }
        } else if (typeObj instanceof BeyahCompta.TransactionType) {
            this.type = (BeyahCompta.TransactionType) typeObj;
        } else {
            System.err.println("Warning: Unexpected type for 'type' field during deserialization. Assigning DEBIT.");
            this.type = BeyahCompta.TransactionType.DEBIT; // Default if unexpected type
        }

        // Handle 'category' field: it could be a String (old version) or TransactionCategory enum (new version)
        Object categoryObj = fields.get("category", null);
        if (categoryObj instanceof String) {
            try {
                this.category = BeyahCompta.TransactionCategory.fromString((String) categoryObj);
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Could not convert old 'category' string '" + categoryObj + "' to enum. Assigning AUTRE.");
                this.category = BeyahCompta.TransactionCategory.AUTRE; // Default to AUTRE if conversion fails
            }
        } else if (categoryObj instanceof BeyahCompta.TransactionCategory) {
            this.category = (BeyahCompta.TransactionCategory) categoryObj;
        } else {
            System.err.println("Warning: Unexpected type for 'category' field during deserialization. Assigning AUTRE.");
            this.category = BeyahCompta.TransactionCategory.AUTRE; // Default if unexpected type
        }
    }
}
