package io.github.SebastianDanielFrenz.WorldEconomyPlugin.market;

import org.bukkit.Location;

import io.github.SebastianDanielFrenz.WorldEconomyPlugin.DataBaseRepresentation;

@DataBaseRepresentation
public class SupplyChestData extends ChestData {

	public SupplyChestData(long ID, Location location, long ownerCompanyID) {
		super(ID, "supply", location);
		this.ownerCompanyID = ownerCompanyID;
	}

	public long ownerCompanyID;

}
