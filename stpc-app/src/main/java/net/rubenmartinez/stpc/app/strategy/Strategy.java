package net.rubenmartinez.stpc.app.strategy;

public interface Strategy {
	public String getStrategyId();
	
	public void activate();
	
	public void deactivate();
	
	public boolean isActivated();
}
