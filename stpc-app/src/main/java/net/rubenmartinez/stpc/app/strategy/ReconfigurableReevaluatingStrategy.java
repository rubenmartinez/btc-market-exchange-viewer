package net.rubenmartinez.stpc.app.strategy;

/**
 * 
 * A Strategy that can be reconfigured at runtime, and it can also re-evaluate past trades.
 * 
 * @param <T> A class representing the StrategyConfiguration (it can be any class that can be serialized from a json)
 */
public interface ReconfigurableReevaluatingStrategy<T> extends Strategy {

	public Class<T> getConfigType();
	
	public T getConfig();
	
	public void reconfigure(T config);
	
	public void reevaluatePastTrades(int i);
}
