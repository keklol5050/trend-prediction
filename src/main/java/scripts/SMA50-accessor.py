from neuralprophet import NeuralProphet, load
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import sys

sizes = {
    "15m": (0.0075, 0.0025),
    "1h": (0.03, 0.01),
    "4h": (0.09, 0.03)
}


def prepare(inp: pd.DataFrame, histr_columns, to_log_columns) -> pd.DataFrame:
    data = inp.copy()

    if pd.isna(data).values.any():
        raise Exception("Nan in data")

    for i in to_log_columns:
        data[i] = np.log2(data[i])

    data = data.rename(columns={'SMA50': 'y'})

    data = data[['ds', 'y'] + histr_columns]

    return data


def load_model(path: str) -> NeuralProphet:
    model = load(path, map_location='cpu')
    return model


if __name__ == "__main__":
    data = pd.read_csv(sys.argv[1])
    model = load_model(sys.argv[2])
    minutes_value = int(sys.argv[3])
    path_to_save_output = sys.argv[4]
    coin = sys.argv[5]
    timeframe = sys.argv[6]

    if coin == 'BTCUSDT':
        to_log_columns = ['count', 'feeValue', 'inputCount', 'inputValue', 'minedValue', 'outputCount', 'outputValue',
                          'fee_avg', 'btc_dom', 'open_interest', 'volume', 'SPX', 'NDX', 'DXY', 'DJI', 'VIX', 'GOLD']
        histr_columns = ['high', 'low', 'volume', 'count', 'feeValue', 'inputCount', 'minedValue', 'outputCount',
                         'fee_avg', 'RSI', 'STOCHK', 'ATR', 'MACD12', 'MACD24', 'CCI', 'ADX', 'DPO', 'MI', 'WILLR',
                         'eth_btc', 'btc_dom', 'open_interest', 'long_short_ratio', 'EMA200', 'SMA200',
                         'WMA200', 'EMA50', 'WMA50', 'VWAP', 'MMA']
    elif coin == 'ETHUSDT':
        to_log_columns = ['gas', 'gasValue', 'amount', 'count', 'gasPrice', 'btc_dom',
                          'open_interest', 'volume', 'SPX', 'NDX', 'DXY', 'DJI', 'VIX', 'GOLD']
        histr_columns = ['high', 'low', 'volume', 'gas', 'gasValue', 'amount', 'count', 'gasPrice', 'RSI',
                         'STOCHK', 'ATR', 'MACD12', 'MACD24', 'CCI', 'ADX', 'DPO', 'MI', 'WILLR', 'eth_btc',
                         'btc_dom', 'open_interest', 'long_short_ratio', 'EMA200', 'SMA200', 'WMA200', 'EMA50',
                         'WMA50', 'VWAP', 'MMA']
    else:
        raise Exception("Undefined coin")

    prepared_data = prepare(data, histr_columns, to_log_columns)

    horizon = model.n_forecasts

    input_df = model.make_future_dataframe(prepared_data, periods=horizon)
    predict = model.predict(input_df, raw=True, decompose=False)

    time_delta = pd.Timedelta(minutes=minutes_value)
    predicted_df = pd.DataFrame({
        'ds': [predict['ds'][0] + i * time_delta for i in range(horizon)],
        'y': [predict[f'step{i}'][0] for i in range(horizon)]
    })

    data['ds'] = pd.to_datetime(data['ds'])
    input_df['ds'] = pd.to_datetime(input_df['ds'])
    input_df.set_index('ds', inplace=True)
    input_df.dropna(inplace=True)

    predicted_df['ds'] = pd.to_datetime(predicted_df['ds'])
    predicted_df.set_index('ds', inplace=True)

    data.set_index('ds', inplace=True)
    data = data[input_df.index[0]:]

    plt.figure(figsize=(20, 7))

    width, width2 = sizes.get(timeframe)

    up = data[data.close >= data.open]
    down = data[data.close < data.open]

    col1 = 'green'
    col2 = 'red'

    plt.bar(up.index, up.close - up.open, width, bottom=up.open, color=col1)
    plt.bar(up.index, up.high - up.close, width2, bottom=up.close, color=col1)
    plt.bar(up.index, up.low - up.open, width2, bottom=up.open, color=col1)

    plt.bar(down.index, down.close - down.open, width, bottom=down.open, color=col2)
    plt.bar(down.index, down.high - down.open, width2, bottom=down.open, color=col2)
    plt.bar(down.index, down.low - down.close, width2, bottom=down.close, color=col2)
    plt.plot(input_df.index, input_df['y'], label='SMA50')
    plt.plot(predicted_df.index, predicted_df['y'], label='SMA50 prediction')

    plt.legend()
    plt.title(f"Trend prediction (SMA50) for {coin} {timeframe}")
    plt.savefig(path_to_save_output)
