package com.shelk;

public class PlayerData {
	
	String workplace;
	long timeJoined;
	double moneyMade;
	
	int amountCollected;
	
	public PlayerData(String workplace, long timeJoined) {
		this.workplace = workplace;
		this.timeJoined = timeJoined;
	}

	public String getWorkplace() {
		return workplace;
	}

	public void setWorkplace(String workplace) {
		this.workplace = workplace;
	}

	public long getTimeJoined() {
		return timeJoined;
	}

	public void setTimeJoined(long timeJoined) {
		this.timeJoined = timeJoined;
	}

	public double getMoneyMade() {
		return moneyMade;
	}

	public void setMoneyMade(double moneyMade) {
		this.moneyMade = moneyMade;
	}

	public int getAmountCollected() {
		return amountCollected;
	}

	public void setAmountCollected(int amountCollected) {
		this.amountCollected = amountCollected;
	}

	
	
	

}
