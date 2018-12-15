function ajaxBase(ajaxParams, callback) {
	request = $.ajax(ajaxParams)
	.done(function(data) {
		if (callback) {
			callback(data);
		}
	}).fail(function(jqXHR, textStatus, errorThrown) {
		if (console && console.error) {
			console.error("ajax failed. textStatus: ["+textStatus+"], errorThrown: ["+errorThrown+"], URL: " + ajaxParams.url);
		}
	});
}

function ajax(url, callback, async) {
	params = { url: url, async: async };
	return ajaxBase(params, callback);	
}

function ajaxJson(url, method, data, callback, async) {
	params = { url: url, data: JSON.stringify(data), async: async, method: method, contentType: 'application/json' };
	return ajaxBase(params, callback);	
}

function refreshDatatable(datatableObject, newTableData) {
	datatableObject.clear().rows.add(newTableData).draw();	
}

function convertOrdersDataToTable(ordersData) {
	var tableData = [];
	for (var i=0; i < ordersData.length; i++) {
		var rowData = {};
		rowData.price = "$" + Number(ordersData[i].price).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
		rowData.amount = Number(ordersData[i].amount).toLocaleString(undefined, {minimumFractionDigits: 8, maximumFractionDigits: 8});
		rowData.value = "$" + Number(ordersData[i].price * ordersData[i].amount).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
		tableData.push(rowData);
	}
	return tableData;
}


function isSimulatedTrade(trade) {
	return trade.tradeTags && trade.tradeTags.hasOwnProperty("SIMULATED_BY");
}

function convertTradesDataToTable(tradesData) {
	var tableData = [];
	for (var i=0; i < tradesData.length; i++) {
		var rowData = {};
		rowData.tradeId = tradesData[i].tradeId;
		rowData.creationDate = tradesData[i].creationDate;
		rowData.price = "$" + Number(tradesData[i].price).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
		rowData.amount = Number(tradesData[i].amount).toLocaleString(undefined, {minimumFractionDigits: 8, maximumFractionDigits: 8});
		rowData.value = "$" + Number(tradesData[i].price * tradesData[i].amount).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
		rowData.makerSide = tradesData[i].makerSide;
		rowData.simulated = isSimulatedTrade(tradesData[i]);
		if (rowData.simulated) {
			rowData.value += " (S)";
		}
		tableData.push(rowData);
	}
	return tableData;
}

function convertTradeObjectToChartPoint(trade) {
	var event = trade;
	
	if (isSimulatedTrade(trade)) {
		if (event.makerSide == "SELL") {
			event.bullet = "triangleUp";
			event.color = "#00cc00";
			event.bulletSize = 10;
		}
		else {
			event.bullet = "triangleDown";
			event.color = "#cc0000";
			event.bulletSize = 10;
		}
	}
	
	return event;
}

function updateChart(chartObject, newElements) {
	var chartData = chartObject.dataProvider;
	if (Array.isArray(newElements)) {
		chartData.push.apply(chartData, newElements);
	}
	else {
		chartData.push(newElements);
	}
	if (chartData.length > TRADES_CHART_NUMBER_OF_POINTS) {
		chartData.splice(0, chartData.length - TRADES_CHART_NUMBER_OF_POINTS);
	}
	
	chartObject.validateData(); // Redraws the chart with new data
}

function regenerateChart(chartObject) {
	chartObject.dataProvider = [];
	var lastTradesUrl = EXCHANGE_LAST_TRADES_ENDPOINT + "?size=" + TRADES_CHART_NUMBER_OF_POINTS + "&strategyId=" + STRATEGY_TEST_ID;
	ajax(lastTradesUrl, function(data) { var pointsFromOlderToNewer = data.reverse().map(convertTradeObjectToChartPoint); updateChart(chartObject, pointsFromOlderToNewer); });
}

$(document).ready(function() {
	var tradesDataTable = $('#lastTrades').DataTable(ordersDataTableOptions);
	var bestBidsDataTable = $('#bestBids').DataTable(ordersDataTableOptions);
	var bestAsksDataTable = $('#bestAsks').DataTable(ordersDataTableOptions);
	
	var tradesChart = AmCharts.makeChart("tradesChart", tradesChartOptions);
	tradesChart.dataProvider = [];
	
	$('#lastTradesSize').val(DEFAULT_TRADES_SIZE);
	$('#bestBidsSize').val(DEFAULT_BIDS_SIZE);
	$('#bestAsksSize').val(DEFAULT_ASKS_SIZE);

	function getLastTradesConfiguredSizeFromDOM() {
		var size = parseInt($('#lastTradesSize').val());
		return size || DEFAULT_TRADES_SIZE;
	}

	function getBestBidsConfiguredSizeFromDOM() {
		var size = parseInt($('#bestBidsSize').val());
		return size || DEFAULT_BIDS_SIZE;
	}

	function getBestAsksConfiguredSizeFromDOM() {
		var size = parseInt($('#bestAsksSize').val());
		return size || DEFAULT_ASKS_SIZE;
	}
	
	var refreshTradesFn = function() {
		var url = EXCHANGE_LAST_TRADES_ENDPOINT + "?size=" + getLastTradesConfiguredSizeFromDOM() + "&strategyId=" + STRATEGY_TEST_ID;
		ajax(url, function(data) { refreshDatatable(tradesDataTable, convertTradesDataToTable(data)); }, true); // async=true as trades can take a long time if requested a very large number
	}

	var refreshBestAsksFn = function() {
		var url = EXCHANGE_BEST_ASKS_ENDPOINT + "?size=" + getBestAsksConfiguredSizeFromDOM();
		ajax(url, function(data) { refreshDatatable(bestAsksDataTable, convertOrdersDataToTable(data)); });
	}

	var refreshBestBidsFn = function() {
		var url = EXCHANGE_BEST_BIDS_ENDPOINT + "?size=" + getBestBidsConfiguredSizeFromDOM();
		ajax(url, function(data) { refreshDatatable(bestBidsDataTable, convertOrdersDataToTable(data)); });
	}
	
	// In case some other client is using the same strategyId than us. TODO: Every client creates each own strategy with an UUID
	var retrieveFromServerAndRefreshStrategyConfigFn = function() {
		var consecutiveUpticksToSellElement = document.getElementById('consecutiveUpticksToSell');
		var consecutiveDownticksToBuyElement = document.getElementById('consecutiveDownticksToBuy');
		if (consecutiveUpticksToSellElement != document.activeElement && consecutiveDownticksToBuyElement != document.activeElement) {
			var url = STRATEGIES_ENDPOINT + "/" + STRATEGY_TEST_ID;
			ajax(url, function(data) {
				config = data.config;
				if (consecutiveUpticksToSellElement.value != config.consecutiveUpticksToSell || consecutiveDownticksToBuyElement.value != config.consecutiveDownticksToBuy) {
					consecutiveUpticksToSellElement.value = config.consecutiveUpticksToSell;
					consecutiveDownticksToBuyElement.value = config.consecutiveDownticksToBuy;
					regenerateChart(tradesChart);
				}
			});
		}
	}
	
	var startTradesWebSocket = function () { // Function so it can be retried with setTimeout
		var tradesWebSocket = new WebSocket(WEBSOCKET_TRADES_ENDPOINT);
		regenerateChart(tradesChart);
		
		tradesWebSocket.onmessage = function(message) { updateChart(tradesChart, convertTradeObjectToChartPoint(JSON.parse(message.data))) };
		tradesWebSocket.onerror = function(error) { console.error("Websocket error: " + error) };
		tradesWebSocket.onclose = function() { setTimeout(function() { startTradesWebSocket(); }, WEBSOCKET_RECONNECT_INTERVAL_MILLIS) };
		
		return tradesWebSocket;
	}

	function refreshStrategyConfig() {
		var url = STRATEGIES_ENDPOINT + "/" + STRATEGY_TEST_ID;
		ajax(url, function(data) {
			config = data.config;
			config.consecutiveUpticksToSell = $('#consecutiveUpticksToSell').val();
			config.consecutiveDownticksToBuy = $('#consecutiveDownticksToBuy').val();
			ajaxJson(url+STRATEGIES_RECONFIGURE, 'PUT', config, function() { regenerateChart(tradesChart); }, false);
		});
	}

	try {
		startTradesWebSocket();
	} catch (err) {
		console.error(err);
		document.getElementById("tradesChart").innerHTML = "Websocket not available for chart";
	}
	
	refreshTradesFn();
	refreshBestAsksFn();
	refreshBestBidsFn();
	retrieveFromServerAndRefreshStrategyConfigFn();
	
	$('#lastTradesSize').change(refreshTradesFn);
	$('#bestAsksSize').change(refreshBestAsksFn);
	$('#bestBidsSize').change(refreshBestBidsFn);
	$('#consecutiveUpticksToSell').change(refreshStrategyConfig);
	$('#consecutiveDownticksToBuy').change(refreshStrategyConfig);
	
	setInterval(refreshTradesFn, TRADES_REFRESH_INTERVAL_MILLIS);
	setInterval(refreshBestAsksFn, BOOK_REFRESH_INTERVAL_MILLIS);
	setInterval(refreshBestBidsFn, BOOK_REFRESH_INTERVAL_MILLIS);
	setInterval(retrieveFromServerAndRefreshStrategyConfigFn, STRATEGY_REFRESH_INTERVAL_MILLIS);
});
