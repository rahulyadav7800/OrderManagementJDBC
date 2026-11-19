import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Scanner;

public class OrderManagementSystem {
	private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
	private static final String USER = "root";
	private static final String PASSWORD = "";

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);

		try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
			System.out.println("Connected to ecommerce_db successfully!");

			while (true) {
				System.out.println("\n--- E-Commerce Order Management System ---");
				System.out.println("1. Insert Order Record (Insert at least 3 orders)");
				System.out.println("2. View Orders by Status (Quantity > 2, Sorted by Unit Price DESC)");
				System.out.println("3. Increase Order Quantity");
				System.out.println("4. Update Order Status");
				System.out.println("5. Display Total Order Value & Delete Cancelled Order");
				System.out.println("6. Exit");
				System.out.print("Enter your choice: ");

				int choice = sc.nextInt();
				sc.nextLine(); // consume newline

				switch (choice) {
					case 1:
						insertOrder(con, sc);
						break;
					case 2:
						viewFilteredOrders(con, sc);
						break;
					case 3:
						increaseQuantity(con, sc);
						break;
					case 4:
						updateOrderStatus(con, sc);
						break;
					case 5:
						totalValueAndDeleteCancelled(con, sc);
						break;
					case 6:
						System.out.println("Exiting application...");
						sc.close();
						return;
					default:
						System.out.println("Invalid choice! Please try again.");
				}
			}
		} catch (SQLException e) {
			System.out.println("Database connection error: " + e.getMessage());
		}
	}

	// Q1(a): Insert order using PreparedStatement and handle duplicate order_id
	private static void insertOrder(Connection con, Scanner sc) {
		String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement pst = con.prepareStatement(sql)) {
			System.out.print("Enter Order ID: ");
			int id = sc.nextInt();
			sc.nextLine();

			System.out.print("Enter Customer Name: ");
			String customerName = sc.nextLine();

			System.out.print("Enter Product Name: ");
			String productName = sc.nextLine();

			System.out.print("Enter Quantity: ");
			int qty = sc.nextInt();

			System.out.print("Enter Unit Price: ");
			double price = sc.nextDouble();
			sc.nextLine();

			System.out.print("Enter Status (Pending/Shipped/Delivered/Cancelled): ");
			String status = sc.nextLine();

			pst.setInt(1, id);
			pst.setString(2, customerName);
			pst.setString(3, productName);
			pst.setInt(4, qty);
			pst.setDouble(5, price);
			pst.setString(6, status);

			int rows = pst.executeUpdate();
			if (rows > 0) {
				System.out.println("Order inserted successfully!");
			}
		} catch (SQLIntegrityConstraintViolationException e) {
			System.out.println("Error: Duplicate Order ID! An order with this ID already exists.");
		} catch (SQLException e) {
			System.out.println("SQL Error during insertion: " + e.getMessage());
		}
	}

	// Q1(b): Filter by status & quantity > 2, sorted by unit_price DESC
	private static void viewFilteredOrders(Connection con, Scanner sc) {
		System.out.print("Enter Status to search: ");
		String status = sc.nextLine();

		String sql = "SELECT * FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";
		try (PreparedStatement pst = con.prepareStatement(sql)) {
			pst.setString(1, status);
			ResultSet rs = pst.executeQuery();

			boolean found = false;
			System.out.println("\nID | Customer | Product | Qty | Unit Price | Status");
			System.out.println("----------------------------------------------------------");
			while (rs.next()) {
				found = true;
				System.out.printf("%d | %s | %s | %d | %.2f | %s%n",
					rs.getInt("order_id"),
					rs.getString("customer_name"),
					rs.getString("product_name"),
					rs.getInt("quantity"),
					rs.getDouble("unit_price"),
					rs.getString("status"));
			}
			if (!found) {
				System.out.println("No orders found matching status '" + status + "' with quantity > 2.");
			}
		} catch (SQLException e) {
			System.out.println("SQL Error: " + e.getMessage());
		}
	}

	// Q1(c): Increase existing quantity by given amount
	private static void increaseQuantity(Connection con, Scanner sc) {
		System.out.print("Enter Order ID: ");
		int id = sc.nextInt();
		System.out.print("Enter Additional Quantity to add: ");
		int addQty = sc.nextInt();
		sc.nextLine();

		String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
		try (PreparedStatement pst = con.prepareStatement(sql)) {
			pst.setInt(1, addQty);
			pst.setInt(2, id);

			int rows = pst.executeUpdate();
			if (rows > 0) {
				System.out.println("Quantity updated successfully for Order ID: " + id);
			} else {
				System.out.println("Order ID " + id + " does not exist.");
			}
		} catch (SQLException e) {
			System.out.println("SQL Error: " + e.getMessage());
		}
	}

	// Q1(d): Retrieve order details, validate status, update and display updated details
	private static void updateOrderStatus(Connection con, Scanner sc) {
		System.out.print("Enter Order ID: ");
		int id = sc.nextInt();
		sc.nextLine();

		String selectSql = "SELECT * FROM Orders WHERE order_id = ?";
		try (PreparedStatement selectPst = con.prepareStatement(selectSql)) {
			selectPst.setInt(1, id);
			ResultSet rs = selectPst.executeQuery();

			if (!rs.next()) {
				System.out.println("Order ID " + id + " does not exist.");
				return;
			}

			System.out.println("Current Details -> Customer: " + rs.getString("customer_name")
					+ ", Product: " + rs.getString("product_name")
					+ ", Qty: " + rs.getInt("quantity")
					+ ", Status: " + rs.getString("status"));

			System.out.print("Enter New Status (Pending / Shipped / Delivered): ");
			String newStatus = sc.nextLine().trim();

			if (!newStatus.equalsIgnoreCase("Pending")
					&& !newStatus.equalsIgnoreCase("Shipped")
					&& !newStatus.equalsIgnoreCase("Delivered")) {
				System.out.println("Invalid status! Allowed values are: Pending, Shipped, Delivered.");
				return;
			}

			// Normalize capitalization
			newStatus = newStatus.substring(0, 1).toUpperCase() + newStatus.substring(1).toLowerCase();

			String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";
			try (PreparedStatement updatePst = con.prepareStatement(updateSql)) {
				updatePst.setString(1, newStatus);
				updatePst.setInt(2, id);
				updatePst.executeUpdate();
				System.out.println("Status updated successfully!");
			}

			// Display updated order details
			ResultSet updatedRs = selectPst.executeQuery();
			if (updatedRs.next()) {
				System.out.printf("Updated Order -> ID: %d | Customer: %s | Product: %s | Qty: %d | Price: %.2f | Status: %s%n",
					updatedRs.getInt("order_id"),
					updatedRs.getString("customer_name"),
					updatedRs.getString("product_name"),
					updatedRs.getInt("quantity"),
					updatedRs.getDouble("unit_price"),
					updatedRs.getString("status"));
			}
		} catch (SQLException e) {
			System.out.println("SQL Error: " + e.getMessage());
		}
	}

	// Q1(e): Display SUM(quantity * unit_price) and delete only if status is Cancelled
	private static void totalValueAndDeleteCancelled(Connection con, Scanner sc) {
		String sumSql = "SELECT SUM(quantity * unit_price) AS total_value FROM Orders";
		try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sumSql)) {
			if (rs.next()) {
				System.out.printf("Total Order Value across all orders: %.2f%n", rs.getDouble("total_value"));
			}
		} catch (SQLException e) {
			System.out.println("SQL Error calculating total value: " + e.getMessage());
		}

		System.out.print("Enter Order ID to delete: ");
		int id = sc.nextInt();
		sc.nextLine();

		String checkSql = "SELECT status FROM Orders WHERE order_id = ?";
		try (PreparedStatement checkPst = con.prepareStatement(checkSql)) {
			checkPst.setInt(1, id);
			ResultSet rs = checkPst.executeQuery();

			if (!rs.next()) {
				System.out.println("Order ID " + id + " does not exist.");
				return;
			}

			String status = rs.getString("status");
			if (!"Cancelled".equalsIgnoreCase(status)) {
				System.out.println("Cannot delete Order ID " + id + ". Only orders with 'Cancelled' status can be deleted (Current status: " + status + ").");
				return;
			}

			String deleteSql = "DELETE FROM Orders WHERE order_id = ?";
			try (PreparedStatement delPst = con.prepareStatement(deleteSql)) {
				delPst.setInt(1, id);
				delPst.executeUpdate();
				System.out.println("Cancelled Order ID " + id + " deleted successfully.");
			}
		} catch (SQLException e) {
			System.out.println("SQL Error during deletion: " + e.getMessage());
		}
	}
}
