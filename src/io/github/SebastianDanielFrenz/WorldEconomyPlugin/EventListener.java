package io.github.SebastianDanielFrenz.WorldEconomyPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import io.github.SebastianDanielFrenz.WorldEconomyPlugin.banking.BankAccount;
import io.github.SebastianDanielFrenz.WorldEconomyPlugin.chatdialogs.CreateBankAccountChatDialog;
import io.github.SebastianDanielFrenz.WorldEconomyPlugin.gui.guis.TradeResourcesGUI;
import io.github.SebastianDanielFrenz.WorldEconomyPlugin.machines.Machine;
import io.github.SebastianDanielFrenz.WorldEconomyPlugin.machines.MachineKategory;
import io.github.SebastianDanielFrenz.WorldEconomyPlugin.mail.MailSubsystem;
import io.github.SebastianDanielFrenz.WorldEconomyPlugin.market.Product;
import io.github.SebastianDanielFrenz.WorldEconomyPlugin.market.ShopSignData;
import io.github.SebastianDanielFrenz.WorldEconomyPlugin.market.SupplyChestData;

public class EventListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) throws SQLException {
		try {
			WorldEconomyProfile profile = WEDB.getUserProfile(event.getPlayer());
			if (profile == null) {
				WEDB.registerUserProfile(event.getPlayer());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		MailSubsystem.playerJoin(event.getPlayer());
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) throws SQLException {
		Player player = event.getPlayer();

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block block = event.getClickedBlock();
			if (block.getType() == Material.OAK_WALL_SIGN) {
				Sign sign = (Sign) block.getState();
				String[] lines = sign.getLines();
				if (lines[0].equalsIgnoreCase("[§4Shop§0]")) {
					if (WorldEconomyCommandExecutor.hasPermission(player, Permissions.SIGN_SHOP_USE)) {
						ShopSignData signData = WEDB.getShopSign(block.getLocation());

						if (signData == null) {
							player.sendMessage("not a world economy sign!");
						} else {
							PlayerInventory playerInv = player.getInventory();
							ItemStack itemStack = playerInv.getItemInMainHand();
							if (itemStack.getType() == Material.PAPER) {
								List<String> lore = itemStack.getItemMeta().getLore();
								if (lore != null) {
									if (lore.get(0).equalsIgnoreCase("Credit Card")) {
										if (lore.size() == 1) {
											event.getPlayer().sendMessage(WorldEconomyPlugin.PREFIX
													+ "§4This credit card has no banking information!");
										}
										String bankAccountName = lore.get(1);
										WorldEconomyProfile profile = WEDB.getUserProfile(player);
										BankAccount bankAccount = WEDB.getBankAccount(profile.bankingID,
												bankAccountName);
										if (bankAccount == null) {
											player.sendMessage(WorldEconomyPlugin.PREFIX
													+ "§4The bank account connected to the credit card does not exist!");
										} else {
											double price = Double.parseDouble(lines[3]);

											if (bankAccount.getBalance() >= price) {
												player.sendMessage(WorldEconomyPlugin.PREFIX
														+ "Your bank account has enough money to buy the item.");
												SupplyChestData chestData = WEDB.getSupplyChest(signData.supplyChestID);
												if (chestData == null) {
													player.sendMessage(WorldEconomyPlugin.PREFIX
															+ "§4The supply chest does not exist!");
												} else {
													Block block2 = chestData.location.getBlock();
													if (block2.getType() == Material.CHEST) {
														Chest chest = (Chest) block2.getState();
														Inventory chestInv = chest.getBlockInventory();

														Product product = WEDB.getProduct(signData.productID);
														if (product == null) {
															player.sendMessage(
																	WorldEconomyPlugin.PREFIX + "§4The product with ID "
																			+ signData.productID + " does not exist!");
														}
														Material productMaterial = Material.getMaterial(product.itemID);

														// test for working
														// banking
														// details

														Company company = WEDB.getCompany(product.manifacturerID);
														if (company == null) {
															player.sendMessage(WorldEconomyPlugin.PREFIX
																	+ "§4The company with ID " + product.manifacturerID
																	+ " does not exist!");
														} else {
															BankAccount companyBankAccount = WEDB
																	.getBankAccount(company.bankingID, "shop_income");
															if (companyBankAccount == null) {
																player.sendMessage(WorldEconomyPlugin.PREFIX
																		+ "§4The company does not have a bank account called \"shop_income\"!");
															} else {

																int itemCount = 0;
																ItemStack chestItemStack;
																for (int i = 0; i < chestInv.getSize(); i++) {
																	chestItemStack = chestInv.getItem(i);
																	if (chestItemStack == null) {
																		continue;
																	}
																	if (chestItemStack.getType() == productMaterial) {
																		itemCount += chestItemStack.getAmount();
																		if (itemCount >= product.itemAmount) {
																			break;
																		}
																	}
																}
																if (itemCount >= product.itemAmount) {
																	// remove
																	// items
																	itemCount = 0;

																	for (int i = 0; i < chestInv.getSize(); i++) {
																		chestItemStack = chestInv.getItem(i);
																		if (chestItemStack == null) {
																			continue;
																		}
																		if (chestItemStack
																				.getType() == productMaterial) {
																			if (product.itemAmount < chestItemStack
																					.getAmount() + itemCount) {
																				chestItemStack.setAmount(
																						chestItemStack.getAmount()
																								- product.itemAmount);
																				break;
																			} else {
																				itemCount += chestItemStack.getAmount();
																				chestInv.setItem(i, null);

																			}
																			if (itemCount == product.itemAmount) {
																				break;
																			}
																		}
																	}

																	// reduce
																	// bank
																	// account
																	// balance

																	WEDB.bankAccountTransaction(bankAccount,
																			companyBankAccount, price);

																	// give
																	// items

																	ItemStack playerItemStack = new ItemStack(
																			productMaterial, product.itemAmount);
																	player.getInventory().addItem(playerItemStack);

																	player.sendMessage(WorldEconomyPlugin.PREFIX
																			+ "Bought " + product.name + " for "
																			+ product.price + "!");
																} else {
																	// not
																	// enough
																	// items
																	// in
																	// chest
																	player.sendMessage(WorldEconomyPlugin.PREFIX
																			+ "§4The supply chest is empty!");
																}
															}
														}
													} else {
														player.sendMessage(WorldEconomyPlugin.PREFIX
																+ "§4The block at the registered supply chest's location is not a chest!");
													}
												}

											} else {
												player.sendMessage(WorldEconomyPlugin.PREFIX
														+ "§4The bank account does not have enough money");
											}
										}
									} else {
										// not a credit card
									}

								} else {
									// no lore

								}
							} else {
								// not paper
							}
						}
					}
				}
			} else if (block.getType() == MachineKategory.CRAFTING.display
					|| block.getType() == MachineKategory.FORGING.display
					|| block.getType() == MachineKategory.SMELTING.display) {
				if (Machine.canBeMachine(block.getType())) {
					Machine machine = Machine.getMachine(block);
					machine.playerUseEvent(player);
					System.out.println("machine event");
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onSignChangeEvent(SignChangeEvent event) throws SQLException {

		Sign sign = (Sign) event.getBlock().getState();
		String[] lines = event.getLines();
		System.out.println(lines[0]);

		if (lines[0].equalsIgnoreCase("[WE - Shop]")) {
			if (WorldEconomyCommandExecutor.hasPermission(event.getPlayer(), Permissions.SIGN_SHOP_CREATE)) {
				long productID = Long.parseLong(lines[1]);
				double price = Double.parseDouble(lines[2]);
				long supplyChestID = Long.parseLong(lines[3]);

				ResultSet r1 = WorldEconomyPlugin.runSQLquery(
						"SELECT productManifacturerID, productName FROM products WHERE productID = " + productID);

				if (r1.next()) {
					long companyID = r1.getLong("productManifacturerID");
					String productName = r1.getString("productName");

					ResultSet r2 = WorldEconomyPlugin
							.runSQLquery("SELECT companyName FROM companies WHERE companyID = " + companyID);
					r2.next();
					String companyName = r2.getString("companyName");

					WorldEconomyPlugin
							.runSQL("INSERT INTO signs (signID, signType, signX, signY, signZ, signWorld) VALUES ("
									+ WEDB.getNextEnumerator("signID") + ", \"shop\", " + sign.getLocation().getBlockX()
									+ ", " + sign.getLocation().getBlockY() + ", " + sign.getLocation().getBlockZ()
									+ ", \"" + sign.getLocation().getWorld().getName() + "\")");
					WorldEconomyPlugin
							.runSQL("INSERT INTO shop_signs (signID, supplyChestID, signOwnerCompanyID, productID, signPrice) VALUES ("
									+ WEDB.getNextEnumerator("signID") + ", " + supplyChestID + ", " + companyID + ", "
									+ productID + ", " + price + ")");

					WEDB.moveEnumerator("signID");

					event.setLine(0, "[§4Shop§0]");
					event.setLine(1, companyName);
					event.setLine(2, productName);
					event.setLine(3, String.valueOf(price));

					event.getPlayer().sendMessage(WorldEconomyPlugin.PREFIX + "Successfully created shop sign!");

				} else {
					event.getPlayer().sendMessage(WorldEconomyPlugin.PREFIX + "§4The product does not exist!");
				}
			}
		}
	}

	@EventHandler
	public void onSignBreakEvent(BlockBreakEvent event) throws SQLException {
		signBreakHandler(event.getBlock());
	}

	@EventHandler
	public void onSignFadeEvent(BlockFadeEvent event) throws SQLException {
		signBreakHandler(event.getBlock());
	}

	@EventHandler
	public void onSignBurnEvent(BlockBurnEvent event) throws SQLException {
		signBreakHandler(event.getBlock());
	}

	@EventHandler
	public void onSignExplodeEvent(BlockExplodeEvent event) throws SQLException {
		signBreakHandler(event.getBlock());
	}

	public static void signBreakHandler(Block block) throws SQLException {
		if (block.getType() == Material.OAK_WALL_SIGN) {
			if (((Sign) block.getState()).getLine(0).equals("[§4Shop§0]")) {
				ShopSignData sign = WEDB.getShopSign(block.getLocation());
				if (sign != null) {
					WEDB.removeShopSign(block);
				}
			}
		}
	}

	@EventHandler
	public void onVillagerInteractEvent(PlayerInteractEntityEvent event) throws SQLException {
		System.out.println("Villager Interact Event!");

		Entity entity = event.getRightClicked();
		if (entity instanceof Villager) {
			Villager villager = (Villager) entity;
			Map<String, String> args;
			args = Utils.getTagsAfter(villager.getScoreboardTags(), "WorldEconomy_Bank_CreateAccount",
					new String[] { "BankName" });
			if (args != null) {
				if (VolatileCooldowns.useVillagerInteractCooldown(event.getPlayer())) {
					new CreateBankAccountChatDialog(event.getPlayer(), WEDB.getBank(args.get("BankName")));
					return;
				}
			}
			args = Utils.getTagsAfter(villager.getScoreboardTags(), "WorldEconomy_Bank_Resources", new String[] {});
			if (args != null) {
				if (VolatileCooldowns.useVillagerInteractCooldown(event.getPlayer())) {
					new TradeResourcesGUI().openInventory(event.getPlayer());
					return;
				}
			}
		}
	}

}
