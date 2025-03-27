import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Menu extends JFrame {
    public static void main(String[] args) {
        try {
            // Set modern Look and Feel (Nimbus)
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Buat object window
        Menu window = new Menu();

        // Atur ukuran window
        window.setSize(600, 700);

        // Letakkan window di tengah layar
        window.setLocationRelativeTo(null);

        // Isi window
        window.setContentPane(window.mainPanel);

        // Tampilkan window
        window.setVisible(true);

        // Agar program ikut berhenti saat window ditutup
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // Index baris yang diklik
    private int selectedIndex = -1;

    // List untuk menampung semua mahasiswa
    private ArrayList<Mahasiswa> listMahasiswa;
    private Database database;

    private JPanel mainPanel;
    private JTextField nimField;
    private JTextField namaField;
    private JTable mahasiswaTable;
    private JButton addUpdateButton;
    private JButton cancelButton;
    private JComboBox<String> jenisKelaminComboBox;
    private JButton deleteButton;
    private JLabel titleLabel;
    private JLabel nimLabel;
    private JLabel namaLabel;
    private JLabel jenisKelaminLabel;
    private JRadioButton SNBTRadioButton;
    private JRadioButton mandiriRadioButton;
    private JRadioButton SNBPRadioButton;
    private JLabel jalurMasukLabel;
    private ButtonGroup jalurMasukGroup;

    // Constructor
    public Menu() {
        // Inisialisasi listMahasiswa
        listMahasiswa = new ArrayList<>();

        database = new Database();
        // Inisialisasi jalurMasukGroup
        jalurMasukGroup = new ButtonGroup();
        jalurMasukGroup.add(SNBTRadioButton);
        jalurMasukGroup.add(SNBPRadioButton);
        jalurMasukGroup.add(mandiriRadioButton);



        // Isi tabel mahasiswa
        mahasiswaTable.setModel(setTable());

        // Atur isi combo box
        String[] jenisKelaminData = {"", "Laki-Laki", "Perempuan"};
        jenisKelaminComboBox.setModel(new DefaultComboBoxModel<>(jenisKelaminData));

        // Sembunyikan button delete
        deleteButton.setVisible(false);

        // Customize UI
        customizeUI();

        // Saat tombol add/update ditekan
        addUpdateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedIndex == -1) {
                    insertData();
                } else {
                    updateData();
                }
            }
        });

        // Saat tombol delete ditekan
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedIndex >= 0) {
                    deleteData();
                }
            }
        });

        // Saat tombol cancel ditekan
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearForm();
            }
        });

        // Saat salah satu baris tabel ditekan
        mahasiswaTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectedIndex = mahasiswaTable.getSelectedRow();

                // Simpan value textfield, combo box, dan radio button
                String selectedNim = mahasiswaTable.getModel().getValueAt(selectedIndex, 1).toString();
                String selectedNama = mahasiswaTable.getModel().getValueAt(selectedIndex, 2).toString();
                String selectedJenisKelamin = mahasiswaTable.getModel().getValueAt(selectedIndex, 3).toString();
                String selectedJalurMasuk = mahasiswaTable.getModel().getValueAt(selectedIndex, 4).toString();

                // Ubah isi textfield, combo box, dan radio button
                nimField.setText(selectedNim);
                namaField.setText(selectedNama);

                // Update jenis kelamin combo box
                jenisKelaminComboBox.setSelectedItem(
                        selectedJenisKelamin.equalsIgnoreCase("Laki-laki") ? "Laki-Laki" : selectedJenisKelamin
                );


                // Set radio button berdasarkan jalur masuk
                switch (selectedJalurMasuk) {
                    case "SNBT":
                        SNBTRadioButton.setSelected(true);
                        break;
                    case "SNBP":
                        SNBPRadioButton.setSelected(true);
                        break;
                    case "Mandiri":
                        mandiriRadioButton.setSelected(true);
                        break;
                }

                // Ubah button "Add" menjadi "Update"
                addUpdateButton.setText("Update");

                // Tampilkan button delete
                deleteButton.setVisible(true);
            }
        });
    }

    public final DefaultTableModel setTable() {
        // Tentukan kolom tabel
        Object[] column = {"No", "NIM", "Nama", "Jenis Kelamin", "Jalur Masuk"};
        DefaultTableModel temp = new DefaultTableModel(null, column);

        listMahasiswa.clear(); // Bersihkan list sebelum diisi ulang

        try {
            ResultSet resultSet = database.selectQuery("SELECT * FROM mahasiswa");
            int i = 0;
            while (resultSet.next()) {
                String nim = resultSet.getString("nim");
                String nama = resultSet.getString("nama");
                String jenisKelamin = resultSet.getString("jenis_kelamin");
                String jalurMasuk = resultSet.getString("jalur_masuk");

                listMahasiswa.add(new Mahasiswa(nim, nama, jenisKelamin, jalurMasuk));

                Object[] row = {i + 1, nim, nama, jenisKelamin, jalurMasuk};
                temp.addRow(row);
                i++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return temp;
    }

    public void insertData() {
        String nim = nimField.getText().trim();
        String nama = namaField.getText().trim();
        String jenisKelamin = jenisKelaminComboBox.getSelectedItem().toString();
        String jalurMasuk = getSelectedJalurMasuk();

        // Validasi input tidak boleh kosong
        if (nim.isEmpty() || nama.isEmpty() || jalurMasuk.isEmpty() || jenisKelamin.isEmpty()) {
            JOptionPane.showMessageDialog(null, "tidak boleh ada data yang kosong!");
            return;
        }

        // Cek apakah NIM sudah ada di database
        String checkQuery = "SELECT COUNT(*) AS count FROM mahasiswa WHERE nim = '" + nim + "'";
        try {
            ResultSet rs = database.selectQuery(checkQuery);
            if (rs.next() && rs.getInt("count") > 0) {
                JOptionPane.showMessageDialog(null, "NIM sudah ada dalam database!");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Query Insert Data ke Database
        String sql = "INSERT INTO mahasiswa (nim, nama, jenis_kelamin, jalur_masuk) VALUES ('"
                + nim + "', '" + nama + "', '" + jenisKelamin + "', '" + jalurMasuk + "')";
        database.insertUpdateDeleteQuery(sql);

        // Update tabel
        mahasiswaTable.setModel(setTable());

        // Bersihkan form
        clearForm();

        JOptionPane.showMessageDialog(null, "Data berhasil ditambahkan!");
    }

    public void updateData() {
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(null, "Pilih data yang ingin diperbarui!");
            return;
        }

        String nim = nimField.getText().trim();
        String nama = namaField.getText().trim();
        String jenisKelamin = jenisKelaminComboBox.getSelectedItem().toString();
        String jalurMasuk = getSelectedJalurMasuk();

        // Validasi input
        if (nim.isEmpty() || nama.isEmpty() || jalurMasuk.isEmpty()) {
            JOptionPane.showMessageDialog(null, "NIM, Nama, dan Jalur Masuk tidak boleh kosong!");
            return;
        }

        // Update Data di Database
        String sql = "UPDATE mahasiswa SET nama = '" + nama + "', jenis_kelamin = '"
                + jenisKelamin + "', jalur_masuk = '" + jalurMasuk + "' WHERE nim = '" + nim + "'";
        database.insertUpdateDeleteQuery(sql);

        // Update tabel
        mahasiswaTable.setModel(setTable());

        // Bersihkan form
        clearForm();

        JOptionPane.showMessageDialog(null, "Data berhasil diperbarui!");
    }

    public void deleteData() {
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(null, "Pilih data yang ingin dihapus!");
            return;
        }

        int option = JOptionPane.showConfirmDialog(
                null, "Apakah Anda yakin ingin menghapus data ini?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            String nim = nimField.getText().trim();

            // Hapus dari Database
            String sql = "DELETE FROM mahasiswa WHERE nim = '" + nim + "'";
            database.insertUpdateDeleteQuery(sql);

            // Update tabel
            mahasiswaTable.setModel(setTable());

            // Bersihkan form
            clearForm();

            JOptionPane.showMessageDialog(null, "Data berhasil dihapus!");
        }
    }


    public void clearForm() {
        // Kosongkan semua textfield, combo box, dan radio button
        nimField.setText("");
        namaField.setText("");
        jenisKelaminComboBox.setSelectedItem("");
        jalurMasukGroup.clearSelection();

        // Ubah button "Update" menjadi "Add"
        addUpdateButton.setText("Add");

        // Sembunyikan button delete
        deleteButton.setVisible(false);

        // Ubah selectedIndex menjadi -1 (tidak ada baris yang dipilih)
        selectedIndex = -1;
    }

    private String getSelectedJalurMasuk() {
        if (SNBTRadioButton.isSelected()) {
            return "SNBT";
        } else if (SNBPRadioButton.isSelected()) {
            return "SNBP";
        } else if (mandiriRadioButton.isSelected()) {
            return "Mandiri";
        } else {
            return ""; // Jika tidak ada yang dipilih
        }
    }

//    private void populateList() {
//        listMahasiswa.add(new Mahasiswa("2203999", "Amelia Zalfa Julianti", "Perempuan", "SNBT"));
//        listMahasiswa.add(new Mahasiswa("2202292", "Muhammad Iqbal Fadhilah", "Laki-laki", "SNBP"));
//        listMahasiswa.add(new Mahasiswa("2202346", "Muhammad Rifky Afandi", "Laki-laki", "Mandiri"));
//        listMahasiswa.add(new Mahasiswa("2210239", "Muhammad Hanif Abdillah", "Laki-laki", "SNBT"));
//        listMahasiswa.add(new Mahasiswa("2202046", "Nurainun", "Perempuan", "SNBP"));
//        listMahasiswa.add(new Mahasiswa("2205101", "Kelvin Julian Putra", "Laki-laki", "Mandiri"));
//        listMahasiswa.add(new Mahasiswa("2200163", "Rifanny Lysara Annastasya", "Perempuan", "SNBT"));
//        listMahasiswa.add(new Mahasiswa("2202869", "Revana Faliha Salma", "Perempuan", "SNBP"));
//        listMahasiswa.add(new Mahasiswa("2209489", "Rakha Dhifiargo Hariadi", "Laki-laki", "Mandiri"));
//        listMahasiswa.add(new Mahasiswa("2203142", "Roshan Syalwan Nurilham", "Laki-laki", "SNBT"));
//        listMahasiswa.add(new Mahasiswa("2200311", "Raden Rahman Ismail", "Laki-laki", "SNBP"));
//        listMahasiswa.add(new Mahasiswa("2200978", "Ratu Syahirah Khairunnisa", "Perempuan", "Mandiri"));
//        listMahasiswa.add(new Mahasiswa("2204509", "Muhammad Fahreza Fauzan", "Laki-laki", "SNBT"));
//        listMahasiswa.add(new Mahasiswa("2205027", "Muhammad Rizki Revandi", "Laki-laki", "SNBP"));
//        listMahasiswa.add(new Mahasiswa("2203484", "Arya Aydin Margono", "Laki-laki", "Mandiri"));
//        listMahasiswa.add(new Mahasiswa("2200481", "Marvel Ravindra Dioputra", "Laki-laki", "SNBT"));
//        listMahasiswa.add(new Mahasiswa("2209889", "Muhammad Fadlul Hafiizh", "Laki-laki", "SNBP"));
//        listMahasiswa.add(new Mahasiswa("2206697", "Rifa Sania", "Perempuan", "Mandiri"));
//        listMahasiswa.add(new Mahasiswa("2207260", "Imam Chalish Rafidhul Haque", "Laki-laki", "SNBT"));
//        listMahasiswa.add(new Mahasiswa("2204343", "Meiva Labibah Putri", "Perempuan", "SNBP"));
//    }

    private void customizeUI() {
        // Set background color
        mainPanel.setBackground(new Color(240, 240, 240));

        // Customize fonts
        Font titleFont = new Font("Arial", Font.BOLD, 24);
        Font labelFont = new Font("Arial", Font.PLAIN, 14);
        Font buttonFont = new Font("Arial", Font.BOLD, 14);

        titleLabel.setFont(titleFont);
        nimLabel.setFont(labelFont);
        namaLabel.setFont(labelFont);
        jenisKelaminLabel.setFont(labelFont);
        jalurMasukLabel.setFont(labelFont);

        addUpdateButton.setFont(buttonFont);
        cancelButton.setFont(buttonFont);
        deleteButton.setFont(buttonFont);

        // Customize button colors
        addUpdateButton.setBackground(new Color(0, 123, 255));
        addUpdateButton.setForeground(Color.white);
        cancelButton.setBackground(new Color(108, 117, 125));
        cancelButton.setForeground(Color.white);
        deleteButton.setBackground(new Color(220, 53, 69));
        deleteButton.setForeground(Color.white);

        // Add padding to text fields and combo box
        nimField.setBorder(BorderFactory.createCompoundBorder(
                nimField.getBorder(),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        namaField.setBorder(BorderFactory.createCompoundBorder(
                namaField.getBorder(),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        jenisKelaminComboBox.setBorder(BorderFactory.createCompoundBorder(
                jenisKelaminComboBox.getBorder(),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Add padding to buttons
        addUpdateButton.setBorder(BorderFactory.createCompoundBorder(
                addUpdateButton.getBorder(),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
                cancelButton.getBorder(),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        deleteButton.setBorder(BorderFactory.createCompoundBorder(
                deleteButton.getBorder(),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // Customize table appearance
        mahasiswaTable.setBackground(new Color(248, 249, 250));
        mahasiswaTable.setGridColor(new Color(222, 226, 230));
        mahasiswaTable.setSelectionBackground(new Color(0, 123, 255));
        mahasiswaTable.setSelectionForeground(Color.white);
    }
}