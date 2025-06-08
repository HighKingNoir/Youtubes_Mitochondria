declare module '@transak/transak-sdk' {
	interface Settings {
		apiKey: string;
		environment: 'STAGING' | 'PRODUCTION';
		defaultCryptoCurrency?: string;
		themeColor?: string;
		exchangeScreenTitle?: string;
		hideMenu?: boolean;
		disableWalletAddressForm?: boolean;
		isFeeCalculationHidden?: boolean;
		isDisableCrypto?: boolean;
		hostURL?: string;
		network?: string;
		networks?: string;
		walletAddress?: string;
		widgetHeight?: string;
		widgetWidth?: string;
		cryptoCurrencyCode?: string;
		fiatCurrency?: string;
	}

	type ModalPopupAllEvents = '*';

	type ModalPopupEvents =
		| 'TRANSAK_ORDER_CANCELLED'
		| 'TRANSAK_ORDER_CREATED'
		| 'TRANSAK_ORDER_FAILED'
		| 'TRANSAK_ORDER_SUCCESSFUL'
		| 'TRANSAK_WIDGET_CLOSE'
		| 'TRANSAK_WIDGET_CLOSE_REQUEST'
		| 'TRANSAK_WIDGET_INITIALISED'
		| 'TRANSAK_WIDGET_OPEN';

	type EventName = ModalPopupEvents | ModalPopupAllEvents;

	export default class Transak {
		public readonly ERROR = 'TRANSAK_ERROR';
		public readonly EVENTS: ModalPopupEvents;
		public readonly ALL_EVENTS: ModalPopupAllEvents;

		constructor(settings: Settings);

		public init(): void;
		public on<T extends EventName>(event: T, callback: (data: Event[T]) => void): void;
		public close(): void;
	}

	interface PaymentOptionField {
		name: string;
		value: string;
	}

	interface PaymentOption {
		currency: string;
		id: string;
		name: string;
		fields: PaymentOptionField[];
	}

	interface StatusHistory {
		status: string;
		createdAt: Date;
		message: string;
		isEmailSentToUser: boolean;
		partnerEventId: string;
	}

	interface Event {
		'*': EventData<'*', boolean | OrderDataStatus>;
		TRANSAK_WIDGET_INITIALISED: EventData<'TRANSAK_WIDGET_INITIALISED', boolean>;
		TRANSAK_WIDGET_OPEN: EventData<'TRANSAK_WIDGET_OPEN', boolean>;
		TRANSAK_ORDER_CREATED: EventData<'TRANSAK_ORDER_CREATED', OrderDataStatus>;
		TRANSAK_ORDER_SUCCESSFUL: EventData<'TRANSAK_ORDER_SUCCESSFUL', OrderDataStatus>;
		TRANSAK_ORDER_FAILED: EventData<'TRANSAK_ORDER_FAILED', OrderDataStatus>;
		TRANSAK_ORDER_CANCELLED: EventData<'TRANSAK_ORDER_CANCELLED', OrderDataStatus>;
		TRANSAK_WIDGET_CLOSE: EventData<'TRANSAK_WIDGET_CLOSE', boolean>;
		TRANSAK_WIDGET_CLOSE_REQUEST: EventData<'TRANSAK_WIDGET_CLOSE_REQUEST', boolean>;
	}

	interface Action {
		status: boolean;
	}

	// Couldn't confirm on that
	interface HTTPErrorResponse {
		error: TransakError;
	}

	interface TransakError {
		statusCode: number;
		name: string;
		message: string;
	}

	type EventStatus = 'PAYMENT_DONE_MARKED_BY_USER' | 'AWAITING_PAYMENT_FROM_USER' | 'PAYMENT_DONE_MARKED_BY_USER';

	type EventData<T extends EventName, D extends boolean | OrderDataStatus> = {
		eventName: T;
		status: D;
	};

	interface OrderDataStatus {
		id: string;
		walletAddress: string;
		createdAt: Date;
		status: EventStatus;
		fiatCurrency: string;
		userId: string;
		cryptoCurrency: string;
		isBuyOrSell: string;
		fiatAmount: number;
		amountPaid: number;
		paymentOptionId: string;
		walletLink: string;
		orderProcessingType: string;
		addressAdditionalData: boolean;
		network: string;
		conversionPrice: number;
		cryptoAmount: number;
		totalFeeInFiat: number;
		fiatAmountInUsd: number;
		referenceCode: number;
		paymentOptions: PaymentOption[];
		autoExpiresAt: Date;
		statusHistories: StatusHistory[];
	}
}