const EXCHANGE_ENDPOINT = "/api/v1/exchange";

const EXCHANGE_BEST_ASKS_ENDPOINT = EXCHANGE_ENDPOINT + "/bestAsks";
const EXCHANGE_BEST_BIDS_ENDPOINT = EXCHANGE_ENDPOINT + "/bestBids";
const EXCHANGE_LAST_TRADES_ENDPOINT = EXCHANGE_ENDPOINT + "/lastTrades";

const STRATEGIES_ENDPOINT = "/api/v1/strategies";
const STRATEGIES_RECONFIGURE = "/reconfigure";
const STRATEGY_TEST_ID	= "contrarian1";

const WEBSOCKET_TRADES_ENDPOINT = 'ws://' + window.location.host + '/trades';
const WEBSOCKET_RECONNECT_INTERVAL_MILLIS = 2000;

const TRADES_REFRESH_INTERVAL_MILLIS = 1000;
const BOOK_REFRESH_INTERVAL_MILLIS = 500;
const STRATEGY_REFRESH_INTERVAL_MILLIS = 2000;

const DEFAULT_TRADES_SIZE = 10;
const DEFAULT_ASKS_SIZE = 10;
const DEFAULT_BIDS_SIZE = 10;
const TRADES_CHART_NUMBER_OF_POINTS = 100;


dataTableCommonOptions = {
	searching : false,
	paging : false,
	ordering : false,
	info : false,
	language: {
		"emptyTable": "Waiting for data"
	},		
	columns: [
		{ "data": "price" },
		{ "data": "amount" },
		{ "data": "value" }
	]
}
	
ordersDataTableOptions = Object.assign(dataTableCommonOptions, {
	"createdRow": function( row, data, dataIndex, cells ) {
		$(row).addClass("markets-table-row");
	}
});
	
tradesDataTableOptions = Object.assign(dataTableCommonOptions, {
	"createdRow": function( row, data, dataIndex, cells ) {
		
		$(row).addClass("markets-table-row");
		if (data.makerSide && data.makerSide === "BUY" ) {
			$(row).addClass("trade-buy");
		}
		else if (data.makerSide && data.makerSide === "SELL" ) {
			$(row).addClass("trade-sell");
		}
		
		if (data.simulated) {
			$(row).addClass("trade-simulated");
		}
	}
});

tradesChartOptions = {
	"type" : "serial",
	"theme" : "light",
	"valueAxes" : [ {
		"id" : "Price",
		//"title": "Price",
		"position" : "left"
	} ],
	"graphs" : [ {
		"id" : "trades",
		"title": "Trades",
		"bullet" : "round",
		"bulletField": "bullet",
		"colorField": "color",
		"bulletSizeField": "bulletSize",
		"bulletBorderThickness": 1,
		"bulletSize": 4,
		"valueField" : "price",
		"balloonText" : "Price: [[price]]"
	} ],
    "legend": {
    	"markerSize": 8,
	    "data": [{
	        "title": "Simulated Buy",
	        "markerType": "triangleUp",
	        "color": "#00cc00",
	    }, {
	        "title": "Simulated Sell",
	        "markerType": "triangleDown",
	        "color": "#cc0000"
	    }]
    },
	"chartCursor": {
		"enabled": true,
	    "categoryBalloonDateFormat": "[DD MMMM] JJ:NN:SS",
	    "cursorPosition": "mouse"
	},
	"chartScrollbar": {
		"enabled": true
	},	
	"categoryField" : "creationDate",
	"categoryAxis" : {
		"parseDates" : true,
		"minPeriod": "ss"
	}
};
