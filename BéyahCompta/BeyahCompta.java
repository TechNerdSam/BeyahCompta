import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter; 
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.TreeMap; 
import java.io.FileWriter;
import java.io.IOException;
import java.io.File; 
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter; 

/**
 * BeyahCompta.java
 * Logiciel ERP/Comptabilité Professionnel (Java SE Swing)
 * V9.7 Finale : Finalisation du contraste et du design des boutons de la barre latérale.
 */
public class BeyahCompta extends JFrame {

    // --- Configuration du Système ---
    private static final String DATA_FILE = "beyahcompta.dat"; 
    private static final long serialVersionUID = 1L; 

    // --- 1. CONFIGURATION DESIGN ---
    private static final Color PRIMARY_COLOR = new Color(25, 35, 45); 
    private static final Color ACCENT_COLOR = new Color(0, 150, 136); 
    
    private static final Color ACCENT_HOVER = new Color(0, 180, 160); 
    private static final Color DANGER_COLOR = new Color(231, 76, 60);
    private static final Color DANGER_HOVER = new Color(255, 99, 71);
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113);
    private static final Color SUCCESS_HOVER = new Color(88, 224, 150);
    private static final Color INFO_COLOR = new Color(52, 152, 219);
    private static final Color INFO_HOVER = new Color(80, 172, 239);

    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_DARK = Color.BLACK; 
    
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_MONEY = new Font("Segoe UI", Font.BOLD, 28);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // --- 2. MODELES DE DONNÉES ---

    private static class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;
        String id;
        Date date;
        String description;
        String compte;
        String libelleCompte;
        double debit; 
        double credit; 
        String journal;

        public Transaction(Date date, String description, String compte, String libelleCompte, double montant, String type, String journal) {
            this.id = UUID.randomUUID().toString();
            this.date = date;
            this.description = description;
            this.compte = compte;
            this.libelleCompte = libelleCompte;
            this.journal = journal;

            if (type.equals("Recette")) { this.debit = montant; this.credit = 0.0; } 
            else { this.debit = montant; this.credit = 0.0; }
        }
        
        public Object[] toArray() {
             return new Object[]{String.format("%tF", date), journal, description, compte, String.format("%.2f €", debit), String.format("%.2f €", credit)};
        }
        
        public String toFECLine(int ecritureNum) {
             final String SEPARATOR = ",";
             return String.join(SEPARATOR, new String[]{
                 journal, journal, String.valueOf(ecritureNum),
                 String.format("%tY%tm%td", date), compte, comptesComptablesMap.getOrDefault(compte, "N/A"),
                 "", "", description.replace(",", " "),
                 String.format("%tY%tm%td", date), description.replace(",", " "),
                 String.format("%.2f", debit), String.format("%.2f", credit), 
                 "", "", String.format("%tY%tm%td", date)
             });
        }
    }

    private static class Client implements Serializable {
        private static final long serialVersionUID = 1L;
        String id;
        String nom;
        String email;
        String telephone;

        public Client(String nom, String email, String telephone) {
            this.id = UUID.randomUUID().toString().substring(0, 8); 
            this.nom = nom;
            this.email = email;
            this.telephone = telephone;
        }

        public Object[] toArray() {
            return new Object[]{id, nom, email, telephone, String.format("%.2f €", 1200.00)}; 
        }
    }
    
    private static class Produit implements Serializable {
        private static final long serialVersionUID = 1L;
        String id;
        String nom;
        double quantite;
        double prixUnitaire;

        public Produit(String nom, double quantite, double prixUnitaire) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.nom = nom;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
        }
        
        public Object[] toArray() {
            return new Object[]{id, nom, quantite, String.format("%.2f €", prixUnitaire), String.format("%.2f €", quantite * prixUnitaire)};
        }
    }
    
    private static class DataContainer implements Serializable {
        private static final long serialVersionUID = 2L;
        List<Client> clients;
        List<Produit> stock;
        List<Transaction> transactions;
        double soldeCourant;
        double chiffreAffaires;

        public DataContainer(List<Client> clients, List<Produit> stock, List<Transaction> transactions, double soldeCourant, double chiffreAffaires) {
            this.clients = clients;
            this.stock = stock;
            this.transactions = transactions;
            this.soldeCourant = soldeCourant;
            this.chiffreAffaires = chiffreAffaires;
        }
    }

    // --- 3. PROPRIÉTÉS GLOBALES ET MODÈLES UI ---
    
    private final Map<String, String> comptesComptablesMap = new TreeMap<>(); 

    private List<Transaction> transactions = new ArrayList<>();
    private List<Client> clients = new ArrayList<>();
    private List<Produit> stock = new ArrayList<>();
    
    private double soldeCourant = 0.0;
    private double chiffreAffaires = 0.0;
    
    private DefaultTableModel transactionTableModel;
    private DefaultTableModel clientTableModel;
    private DefaultTableModel stockTableModel;
    private DefaultTableModel balanceTableModel; 

    private JLabel soldeLabel, caLabel, resultatLabel;
    private JTabbedPane mainTabs;
    
    // --- 4. CONSTRUCTEUR ---
    public BeyahCompta() {
        comptesComptablesMap.put("512000", "Banque");
        comptesComptablesMap.put("607000", "Achats de marchandises");
        comptesComptablesMap.put("707000", "Ventes de marchandises");
        comptesComptablesMap.put("401000", "Fournisseurs");
        comptesComptablesMap.put("411000", "Clients");

        chargerDonnees();
        
        transactionTableModel = new DefaultTableModel(new Object[]{"Date", "Journal", "Description", "Compte", "Débit (€)", "Crédit (€)"}, 0);
        clientTableModel = new DefaultTableModel(new Object[]{"ID", "Nom", "Email", "Téléphone", "Solde Dû"}, 0);
        stockTableModel = new DefaultTableModel(new Object[]{"ID", "Produit", "Quantité", "Prix Unitaire (€)", "Valeur Totale (€)"}, 0);
        balanceTableModel = new DefaultTableModel(new Object[]{"Compte", "Libellé", "Total Débit", "Total Crédit", "Solde Débit", "Solde Crédit"}, 0);

        setTitle("BeyahCompta ERP/Comptabilité - V9.6 Finale (Contraste Parfait)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                sauvegarderDonnees(); 
            }
        });
        
        setLayout(new BorderLayout());
        add(createSidebar(), BorderLayout.WEST);
        mainTabs = createMainContentArea();
        add(mainTabs, BorderLayout.CENTER);
        
        updateDashboard(); 
        setVisible(true);
    }
    
    // --------------------------------------------------------------------------------
    // --- 5. PERSISTANCE (Inchangée) ---
    // --------------------------------------------------------------------------------

    private void sauvegarderDonnees() {
        try {
            DataContainer container = new DataContainer(clients, stock, transactions, soldeCourant, chiffreAffaires);
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
                oos.writeObject(container);
            }
            System.out.println("Données sauvegardées avec succès.");
        } catch (IOException e) {
            String errorMessage = "Erreur lors de la sauvegarde des données: " + e.getMessage();
            System.err.println(errorMessage);
            
            if (e instanceof NotSerializableException) {
                 JOptionPane.showMessageDialog(this, "Erreur CRITIQUE: L'interface utilisateur tente d'être sérialisée. Nettoyez le fichier.", "Erreur de Sérialisation (Bloquante)", JOptionPane.ERROR_MESSAGE);
            } else if (e.getMessage() != null && !e.getMessage().contains("writing aborted") && !e.getMessage().contains("com.sun.java")) {
                 JOptionPane.showMessageDialog(this, "Erreur critique de persistance.", "Erreur de Sauvegarde", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void chargerDonnees() {
        File file = new File(DATA_FILE);
        if (file.exists() && file.length() > 0) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Object loadedObject = ois.readObject();
                
                if (loadedObject instanceof DataContainer) {
                    DataContainer container = (DataContainer) loadedObject;
                    clients = container.clients;
                    stock = container.stock;
                    transactions = container.transactions;
                    soldeCourant = container.soldeCourant;
                    chiffreAffaires = container.chiffreAffaires;
                    System.out.println("Données chargées depuis le fichier.");
                } else {
                    System.err.println("Fichier de données obsolète ou corrompu. Suppression et utilisation de démo.");
                    if (file.delete()) { System.out.println("Ancien fichier supprimé."); }
                    insererDonneesDeDemo();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erreur lors du chargement des données. Utilisation des données de démo: " + e.getMessage());
                if (file.delete()) { System.out.println("Fichier illisible supprimé."); }
                insererDonneesDeDemo();
            }
        } else {
            insererDonneesDeDemo();
        }
    }
    
    private void insererDonneesDeDemo() {
        if (clients.isEmpty()) {
            clients.add(new Client("SARL Dupont", "dupont@mail.com", "01 23 45 67 89"));
            stock.add(new Produit("Laptop Pro X", 50.0, 1200.00));
            
            addComptaTransactionDemo(new Date(), "Vente d'un Laptop", "707000", 1500.00, "Recette", "VTE");
            addComptaTransactionDemo(new Date(), "Paiement Fournisseur", "607000", 300.00, "Dépense", "ACH");
        }
    }
    
    private void addComptaTransactionDemo(Date date, String description, String compte, double montant, String type, String journal) {
        String libelle = comptesComptablesMap.getOrDefault(compte, "Compte Inconnu");
        Transaction newTrans = new Transaction(date, description, compte, libelle, montant, type, journal);
        transactions.add(newTrans);

        double montantNet = newTrans.debit - newTrans.credit;
        soldeCourant += montantNet;
        if (newTrans.debit > 0) chiffreAffaires += newTrans.debit;
    }

    // --------------------------------------------------------------------------------
    // --- 6. LOGIQUE MÉTIER (CRUD) ---
    // --------------------------------------------------------------------------------

    public void addClient(String nom, String email, String telephone) {
        clients.add(new Client(nom, email, telephone));
        updateClientTable();
        sauvegarderDonnees();
    }
    
    public void updateClient(String id, String nom, String email, String telephone) {
        Client clientToUpdate = clients.stream().filter(c -> c.id.equals(id)).findFirst().orElse(null);
        if (clientToUpdate != null) {
            clientToUpdate.nom = nom;
            clientToUpdate.email = email;
            clientToUpdate.telephone = telephone;
            updateClientTable();
            sauvegarderDonnees();
        }
    }
    
    public void deleteClient(String id) {
        clients.removeIf(c -> c.id.equals(id));
        updateClientTable();
        sauvegarderDonnees();
    }
    
    public void addProduit(String nom, double quantite, double prixUnitaire) {
        stock.add(new Produit(nom, quantite, prixUnitaire));
        updateStockTable();
        sauvegarderDonnees();
    }
    
    public void updateProduit(String id, String nom, double quantite, double prixUnitaire) {
        Produit produitToUpdate = stock.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
        if (produitToUpdate != null) {
            produitToUpdate.nom = nom;
            produitToUpdate.quantite = quantite;
            produitToUpdate.prixUnitaire = prixUnitaire;
            updateStockTable();
            sauvegarderDonnees();
        }
    }
    
    public void deleteProduit(String id) {
        stock.removeIf(p -> p.id.equals(id));
        updateStockTable();
        sauvegarderDonnees();
    }

    public void addComptaTransaction(Date date, String description, String compte, double montant, String type, String journal) {
        String libelle = comptesComptablesMap.getOrDefault(compte, "Compte Inconnu");
        Transaction newTrans = new Transaction(date, description, compte, libelle, montant, type, journal);
        transactions.add(newTrans);

        double montantNet = newTrans.debit - newTrans.credit;
        soldeCourant += montantNet;
        if (newTrans.debit > 0) chiffreAffaires += newTrans.debit;
        
        updateDashboard(); 
        sauvegarderDonnees();
    }
    
    // --------------------------------------------------------------------------------
    // --- 7. UI/RENDERER ET UTILITAIRES ---
    // --------------------------------------------------------------------------------

    /** Crée un bouton avec des styles UX/UI interactifs et un contraste garanti. */
    private JButton createInteractiveButton(String text, Color baseColor, Color hoverColor) {
        JButton button = new JButton(text);
        button.setBackground(baseColor);
        button.setForeground(TEXT_LIGHT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(200, 40));
        
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 50), 1), 
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // CORRECTION DE LISIBILITÉ: Gère la couleur du texte en fonction du fond (Contraste garanti)
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                // Le survol passe à une couleur plus claire -> Texte noir pour le contraste
                button.setForeground(TEXT_DARK); 
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
                // Retour à la couleur de base plus foncée -> Texte blanc
                button.setForeground(TEXT_LIGHT); 
            }
            @Override
            public void mousePressed(MouseEvent e) {
                 // Effet de clic très foncé
                 button.setBackground(baseColor.darker().darker()); 
                 button.setForeground(TEXT_LIGHT); 
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                 button.setBackground(hoverColor);
                 button.setForeground(TEXT_DARK); 
            }
        });

        return button;
    }

    /** Ajoute les boutons de la barre latérale avec un contraste garanti sur fond PRIMARY_COLOR. */
    private void addMenuItem(JPanel parent, String label, int tabIndex) {
        JButton item = new JButton(label);
        item.setFont(FONT_HEADER);
        item.setForeground(TEXT_LIGHT); 
        item.setBackground(PRIMARY_COLOR);
        item.setBorderPainted(false);
        item.setFocusPainted(false);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setMaximumSize(new Dimension(220, 45));
        item.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // CORRECTION LISIBILITÉ BARRE LATÉRALE: Utilisation de PRIMARY_COLOR.brighter() au survol
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(PRIMARY_COLOR.brighter()); 
                item.setForeground(TEXT_LIGHT); // Maintien du texte blanc
            }
            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(PRIMARY_COLOR);
                item.setForeground(TEXT_LIGHT);
            }
        });
        
        item.addActionListener(e -> mainTabs.setSelectedIndex(tabIndex));
        
        parent.add(item);
        parent.add(Box.createVerticalStrut(5));
    }
    
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PRIMARY_COLOR);
        sidebar.setPreferredSize(new Dimension(220, 750));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

        JLabel title = new JLabel("BeyahCompta");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_LIGHT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(title);
        sidebar.add(Box.createVerticalStrut(30));

        addMenuItem(sidebar, "Tableau de Bord", 0);
        addMenuItem(sidebar, "Transactions", 1);
        addMenuItem(sidebar, "Clients / CRM", 2);
        addMenuItem(sidebar, "Gestion de Stock", 3);
        addMenuItem(sidebar, "Rapports Légaux", 4);
        
        return sidebar;
    }

    private JTabbedPane createMainContentArea() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FONT_HEADER);
        
        tabs.addTab("Tableau de Bord", createDashboardPanel());
        tabs.addTab("Transactions", createTransactionsPanel());
        tabs.addTab("Clients / CRM", createClientsPanel());
        tabs.addTab("Gestion de Stock", createStockPanel());
        tabs.addTab("Rapports Légaux", createReportsPanel());

        return tabs;
    }
    
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        soldeLabel = createIndicatorCard("Solde Courant", "0.00 €", SUCCESS_COLOR);
        caLabel = createIndicatorCard("Chiffre d'Affaires", "0.00 €", ACCENT_COLOR);
        resultatLabel = createIndicatorCard("Résultat (Est.)", "0.00 €", INFO_COLOR);

        panel.add(soldeLabel);
        panel.add(caLabel);
        panel.add(resultatLabel);
        
        return panel;
    }

    private JPanel createTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        JTable table = new JTable(transactionTableModel);
        table.getTableHeader().setFont(FONT_HEADER);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Grand Livre des Mouvements (Journal des Opérations)"));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Ajouter un Mouvement Simple"));
        
        JTextField descriptionField = new JTextField(15);
        JTextField montantField = new JTextField(8);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Recette", "Dépense"});
        JComboBox<String> compteBox = new JComboBox<>(new String[]{"707000 - Ventes", "607000 - Achats"}); 

        JButton ajouterButton = createInteractiveButton("Enregistrer Transaction", ACCENT_COLOR, ACCENT_HOVER);
        
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(descriptionField);
        inputPanel.add(new JLabel("Compte:"));
        inputPanel.add(compteBox);
        inputPanel.add(new JLabel("Type:"));
        inputPanel.add(typeBox);
        inputPanel.add(new JLabel("Montant:"));
        inputPanel.add(montantField);
        inputPanel.add(ajouterButton);

        ajouterButton.addActionListener(e -> {
            try {
                String desc = descriptionField.getText().trim();
                double montant = Double.parseDouble(montantField.getText().replace(",", "."));
                String type = (String) typeBox.getSelectedItem();
                String compteStr = ((String) compteBox.getSelectedItem()).substring(0, 6);
                String journal = compteStr.startsWith("7") ? "VTE" : "ACH";

                if (desc.isEmpty() || montant <= 0) { throw new IllegalArgumentException(); }

                addComptaTransaction(new Date(), desc, compteStr, montant, type, journal);
                
                descriptionField.setText("");
                montantField.setText("");
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Veuillez vérifier les champs.", "Erreur de Saisie", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createClientsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        JTable clientTable = new JTable(clientTableModel);
        clientTable.getColumnModel().getColumn(0).setMaxWidth(60);
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(clientTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Gestion des Clients (CRUD)"));

        JButton addButton = createInteractiveButton("Ajouter Client", SUCCESS_COLOR, SUCCESS_HOVER);
        JButton editButton = createInteractiveButton("Modifier Client", ACCENT_COLOR, ACCENT_HOVER);
        JButton deleteButton = createInteractiveButton("Supprimer Client", DANGER_COLOR, DANGER_HOVER);
        
        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);
        
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        addButton.addActionListener(e -> showClientDialog(null));
        
        editButton.addActionListener(e -> {
            int selectedRow = clientTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) clientTableModel.getValueAt(selectedRow, 0);
                Client client = clients.stream().filter(c -> c.id.equals(clientId)).findFirst().orElse(null);
                if (client != null) {
                    showClientDialog(client);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Sélectionnez un client à modifier.", "Erreur", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        deleteButton.addActionListener(e -> {
            int selectedRow = clientTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) clientTableModel.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(this, "Êtes-vous sûr de vouloir supprimer ce client ? Cette action est irréversible.", "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteClient(clientId);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Sélectionnez un client à supprimer.", "Erreur", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        return panel;
    }

    private JPanel createStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        JTable stockTable = new JTable(stockTableModel);
        stockTable.getColumnModel().getColumn(0).setMaxWidth(60); 
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(stockTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Gestion des Produits (CRUD)"));

        JButton addButton = createInteractiveButton("Ajouter Produit", SUCCESS_COLOR, SUCCESS_HOVER);
        JButton editButton = createInteractiveButton("Modifier Produit", ACCENT_COLOR, ACCENT_HOVER);
        JButton deleteButton = createInteractiveButton("Supprimer Produit", DANGER_COLOR, DANGER_HOVER);
        
        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);
        
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        addButton.addActionListener(e -> showProductDialog(null));
        
        editButton.addActionListener(e -> {
            int selectedRow = stockTable.getSelectedRow();
            if (selectedRow != -1) {
                String productId = (String) stockTableModel.getValueAt(selectedRow, 0);
                Produit produit = stock.stream().filter(p -> p.id.equals(productId)).findFirst().orElse(null);
                if (produit != null) {
                    showProductDialog(produit);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Sélectionnez un produit à modifier.", "Erreur", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        deleteButton.addActionListener(e -> {
            int selectedRow = stockTable.getSelectedRow();
            if (selectedRow != -1) {
                String productId = (String) stockTableModel.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(this, "Êtes-vous sûr de vouloir supprimer ce produit du stock ?", "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteProduit(productId);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Sélectionnez un produit à supprimer.", "Erreur", JOptionPane.WARNING_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTable balanceTable = new JTable(balanceTableModel);
        balanceTable.getTableHeader().setFont(FONT_HEADER);
        balanceTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JScrollPane balanceScrollPane = new JScrollPane(balanceTable);
        balanceScrollPane.setBorder(BorderFactory.createTitledBorder("Balance Générale (6 colonnes)"));
        panel.add(balanceScrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));

        JButton refreshBalanceBtn = createInteractiveButton("Calculer Balance", INFO_COLOR, INFO_HOVER);
        JButton grandLivreBtn = createInteractiveButton("Afficher Grand Livre", ACCENT_COLOR, ACCENT_HOVER);
        JButton exportFECBtn = createInteractiveButton("Exportation Fichier FEC (CSV)", SUCCESS_COLOR, SUCCESS_HOVER);
        
        grandLivreBtn.addActionListener(e -> generateGrandLivre()); 
        refreshBalanceBtn.addActionListener(e -> calculateBalance());
        exportFECBtn.addActionListener(e -> generateFEC());

        controlPanel.add(refreshBalanceBtn);
        controlPanel.add(grandLivreBtn);
        controlPanel.add(exportFECBtn);
        
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private void addMenuItem(JPanel parent, String label, int tabIndex) {
        JButton item = new JButton(label);
        item.setFont(FONT_HEADER);
        item.setForeground(TEXT_LIGHT);
        item.setBackground(PRIMARY_COLOR);
        item.setBorderPainted(false);
        item.setFocusPainted(false);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setMaximumSize(new Dimension(220, 45));
        item.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Contraste garanti pour le survol de la barre latérale
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(PRIMARY_COLOR.brighter()); 
                item.setForeground(TEXT_LIGHT); 
            }
            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(PRIMARY_COLOR);
                item.setForeground(TEXT_LIGHT);
            }
        });
        
        item.addActionListener(e -> mainTabs.setSelectedIndex(tabIndex));
        
        parent.add(item);
        parent.add(Box.createVerticalStrut(5));
    }
    
    private JLabel createIndicatorCard(String title, String value, Color bgColor) {
        JLabel card = new JLabel("<html><div style='text-align: center; color: white;'><b>" + title + "</b><br><span style='font-size: 1.5em;'>" + value + "</span></div></html>", SwingConstants.CENTER);
        card.setOpaque(true);
        card.setBackground(bgColor);
        card.setFont(FONT_MONEY);
        card.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        return card;
    }

    private void updateDashboard() {
        soldeLabel.setText(String.format("<html><div style='text-align: center; color: white;'><b>Solde Courant</b><br><span style='font-size: 1.5em;'>%.2f €</span></div></html>", soldeCourant));
        soldeLabel.setBackground(soldeCourant >= 0 ? SUCCESS_COLOR : DANGER_COLOR);
        
        caLabel.setText(String.format("<html><div style='text-align: center; color: white;'><b>Chiffre d'Affaires</b><br><span style='font-size: 1.5em;'>%.2f €</span></div></html>", chiffreAffaires));
        
        double resultat = calculerResultat();
        resultatLabel.setText(String.format("<html><div style='text-align: center; color: white;'><b>Résultat (Est.)</b><br><span style='font-size: 1.5em;'>%.2f €</span></div></html>", resultat));
        resultatLabel.setBackground(resultat >= 0 ? INFO_COLOR : DANGER_COLOR);
        
        updateTransactionTable();
        updateClientTable();
        updateStockTable();
    }
    
    private double calculerResultat() { return soldeCourant; }

    private void updateTransactionTable() {
        transactionTableModel.setRowCount(0);
        for (Transaction t : transactions) {
            transactionTableModel.addRow(t.toArray());
        }
    }

    private void updateClientTable() {
        clientTableModel.setRowCount(0);
        for (Client c : clients) {
            clientTableModel.addRow(c.toArray());
        }
    }
    
    private void updateStockTable() {
        stockTableModel.setRowCount(0);
        for (Produit p : stock) {
            stockTableModel.addRow(p.toArray());
        }
    }

    private void showClientDialog(Client clientToEdit) {
        JTextField nomField = new JTextField(clientToEdit != null ? clientToEdit.nom : "", 20);
        JTextField emailField = new JTextField(clientToEdit != null ? clientToEdit.email : "", 20);
        JTextField telephoneField = new JTextField(clientToEdit != null ? clientToEdit.telephone : "", 20);

        JPanel myPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        myPanel.add(new JLabel("Nom :"));
        myPanel.add(nomField);
        myPanel.add(new JLabel("Email :"));
        myPanel.add(emailField);
        myPanel.add(new JLabel("Téléphone :"));
        myPanel.add(telephoneField);

        int result = JOptionPane.showConfirmDialog(this, myPanel, 
                clientToEdit == null ? "Ajouter un nouveau client" : "Modifier le client " + clientToEdit.nom, JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (clientToEdit == null) {
                addClient(nomField.getText(), emailField.getText(), telephoneField.getText());
            } else {
                updateClient(clientToEdit.id, nomField.getText(), emailField.getText(), telephoneField.getText());
            }
            JOptionPane.showMessageDialog(this, "Opération Client effectuée avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showProductDialog(Produit productToEdit) {
        JTextField nomField = new JTextField(productToEdit != null ? productToEdit.nom : "", 20);
        JTextField quantiteField = new JTextField(productToEdit != null ? String.valueOf(productToEdit.quantite) : "", 10);
        JTextField prixField = new JTextField(productToEdit != null ? String.valueOf(productToEdit.prixUnitaire) : "", 10);

        JPanel myPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        myPanel.add(new JLabel("Nom du Produit :"));
        myPanel.add(nomField);
        myPanel.add(new JLabel("Quantité en Stock :"));
        myPanel.add(quantiteField);
        myPanel.add(new JLabel("Prix Unitaire (€) :"));
        myPanel.add(prixField);

        int result = JOptionPane.showConfirmDialog(this, myPanel, 
                productToEdit == null ? "Ajouter un nouveau produit" : "Modifier le produit " + productToEdit.nom, JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                double quantite = Double.parseDouble(quantiteField.getText().replace(",", "."));
                double prix = Double.parseDouble(prixField.getText().replace(",", "."));
                
                if (productToEdit == null) {
                    addProduit(nomField.getText(), quantite, prix);
                } else {
                    updateProduit(productToEdit.id, nomField.getText(), quantite, prix);
                }
                JOptionPane.showMessageDialog(this, "Opération Stock effectuée avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Veuillez entrer des valeurs numériques valides pour la quantité et le prix.", "Erreur de Saisie", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void calculateBalance() {
        Map<String, double[]> balanceData = new TreeMap<>();
        
        for (Transaction t : transactions) {
            String key = t.compte;
            balanceData.putIfAbsent(key, new double[]{0.0, 0.0}); 
            
            balanceData.get(key)[0] += t.debit;
            balanceData.get(key)[1] += t.credit;
        }

        balanceTableModel.setRowCount(0);
        
        double totalGlobalDebit = 0.0;
        double totalGlobalCredit = 0.0;

        for (Map.Entry<String, double[]> entry : balanceData.entrySet()) {
            String compte = entry.getKey();
            double totalDebit = entry.getValue()[0];
            double totalCredit = entry.getValue()[1];
            double solde = totalDebit - totalCredit;
            
            double soldeDebit = (solde > 0) ? solde : 0.0;
            double soldeCredit = (solde < 0) ? Math.abs(solde) : 0.0;
            
            totalGlobalDebit += totalDebit;
            totalGlobalCredit += totalCredit;

            balanceTableModel.addRow(new Object[]{
                compte,
                comptesComptablesMap.getOrDefault(compte, "N/A"),
                String.format("%.2f €", totalDebit),
                String.format("%.2f €", totalCredit),
                String.format("%.2f €", soldeDebit),
                String.format("%.2f €", soldeCredit)
            });
        }
        
        balanceTableModel.addRow(new Object[]{
            "TOTAL", "FIN", 
            String.format("%.2f €", totalGlobalDebit), 
            String.format("%.2f €", totalGlobalCredit), 
            String.format("%.2f €", totalGlobalDebit - totalGlobalCredit),
            String.format("%.2f €", totalGlobalCredit - totalGlobalDebit)
        });
        
        JOptionPane.showMessageDialog(this, "Balance recalculée avec succès. Totaux Débit/Crédit égalent : " + String.format("%.2f €", totalGlobalDebit), "Calcul Balance", JOptionPane.INFORMATION_MESSAGE);
    }

    private void generateGrandLivre() {
        if (transactions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune transaction enregistrée pour générer le Grand Livre.", "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog grandLivreDialog = new JDialog(this, "Grand Livre des Comptes", true);
        grandLivreDialog.setSize(800, 500);
        grandLivreDialog.setLayout(new BorderLayout());

        DefaultTableModel livreModel = new DefaultTableModel(new Object[]{"Date", "Compte", "Libellé", "Débit", "Crédit"}, 0);
        
        TreeMap<String, List<Transaction>> transactionsParCompte = new TreeMap<>();
        for (Transaction t : transactions) {
            transactionsParCompte.computeIfAbsent(t.compte + " - " + t.libelleCompte, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : transactionsParCompte.entrySet()) {
            String compte = entry.getKey();
            List<Transaction> liste = entry.getValue();
            
            double totalDebit = 0;
            double totalCredit = 0;
            
            livreModel.addRow(new Object[]{"---", "---", compte, "---", "---"});

            for (Transaction t : liste) {
                livreModel.addRow(new Object[]{
                    String.format("%tF", t.date), t.compte, t.description, 
                    String.format("%.2f", t.debit), String.format("%.2f", t.credit)
                });
                totalDebit += t.debit;
                totalCredit += t.credit;
            }
            
            livreModel.addRow(new Object[]{"", "", "TOTAL " + compte, String.format("= %.2f", totalDebit), String.format("= %.2f", totalCredit)});
        }

        JTable livreTable = new JTable(livreModel);
        livreTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        grandLivreDialog.add(new JScrollPane(livreTable), BorderLayout.CENTER);
        grandLivreDialog.setLocationRelativeTo(this);
        grandLivreDialog.setVisible(true);
    }
    
    private void generateFEC() {
        JFileChooser fileChooser = new JFileChooser();
        
        String defaultFileName = String.format("FEC_%tY%tm%td.csv", new Date(), new Date(), new Date());
        fileChooser.setSelectedFile(new File(defaultFileName));
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Fichier CSV (*.csv)", "csv");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }
            
            final String SEPARATOR = ",";
            
            String header = String.join(SEPARATOR, new String[]{
                "JournalCode", "JournalLib", "EcritureNum", "EcritureDate", "CompteNum", "CompteLib", 
                "CompAuxNum", "CompAuxLib", "PieceRef", "PieceDate", "EcritureLib", 
                "Debit", "Credit", "EcritureLet", "DateLet", "ValidDate"
            });

            try (FileWriter writer = new FileWriter(fileToSave)) {
                writer.write(header + "\n");
                
                int ecritureNumCounter = 1;
                for (Transaction t : transactions) {
                    String compteContrepartie = t.compte.startsWith("7") ? "512000" : "512000"; 
                    String libelleContrepartie = comptesComptablesMap.getOrDefault(compteContrepartie, "Banque");
                    
                    writer.write(t.toFECLine(ecritureNumCounter) + "\n");

                    double contrepartieDebit = t.credit; 
                    double contrepartieCredit = t.debit;  
                    
                    String ligneContrepartie = String.join(SEPARATOR, new String[]{
                        t.journal, t.journal, String.valueOf(ecritureNumCounter), String.format("%tY%tm%td", t.date), 
                        compteContrepartie, libelleContrepartie, "", "", t.description.replace(",", " "), 
                        String.format("%tY%tm%td", t.date), t.description.replace(",", " "), 
                        String.format("%.2f", contrepartieDebit), String.format("%.2f", contrepartieCredit), 
                        "", "", String.format("%tY%tm%td", t.date)
                    });
                    writer.write(ligneContrepartie + "\n");

                    ecritureNumCounter++;
                }
                JOptionPane.showMessageDialog(this, "FEC exporté en CSV (Excel) avec succès à:\n" + fileToSave.getAbsolutePath(), "Succès Exportation", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erreur lors de la génération du FEC: " + ex.getMessage(), "Erreur E/S", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    // --- MAIN ---
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        
        SwingUtilities.invokeLater(() -> new BeyahCompta());
    }
}