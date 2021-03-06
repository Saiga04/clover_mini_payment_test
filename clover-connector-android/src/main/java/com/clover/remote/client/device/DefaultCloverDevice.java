/*
 * Copyright (C) 2016 Clover Network, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.clover.remote.client.device;

import com.clover.remote.KeyPress;
import com.clover.remote.client.CloverDeviceObserver;
import com.clover.remote.client.transport.CloverTransport;
import com.clover.remote.client.transport.CloverTransportObserver;
import com.clover.remote.message.BreakMessage;
import com.clover.remote.message.CapturePreAuthMessage;
import com.clover.remote.message.CapturePreAuthResponseMessage;
import com.clover.remote.message.CashbackSelectedMessage;
import com.clover.remote.message.CloseoutRequestMessage;
import com.clover.remote.message.CloseoutResponseMessage;
import com.clover.remote.message.DiscoveryRequestMessage;
import com.clover.remote.message.DiscoveryResponseMessage;
import com.clover.remote.message.FinishOkMessage;
import com.clover.remote.message.ImagePrintMessage;
import com.clover.remote.message.KeyPressMessage;
import com.clover.remote.message.Message;
import com.clover.remote.message.Method;
import com.clover.remote.message.OpenCashDrawerMessage;
import com.clover.remote.message.OrderUpdateMessage;
import com.clover.remote.message.PartialAuthMessage;
import com.clover.remote.message.RefundRequestMessage;
import com.clover.remote.message.RefundResponseMessage;
import com.clover.remote.message.RemoteMessage;
import com.clover.remote.message.ShowPaymentReceiptOptionsMessage;
//import com.clover.remote.message.ShowRefundReceiptOptionsMessage;
//import com.clover.remote.message.ShowManualRefundReceiptOptionsMessage;
import com.clover.remote.message.SignatureVerifiedMessage;
import com.clover.remote.message.TerminalMessage;
import com.clover.remote.message.TextPrintMessage;
import com.clover.remote.message.ThankYouMessage;
import com.clover.remote.message.TipAddedMessage;
import com.clover.remote.message.TipAdjustMessage;
import com.clover.remote.message.TipAdjustResponseMessage;
import com.clover.remote.message.TxStartRequestMessage;
import com.clover.remote.message.TxStateMessage;
import com.clover.remote.message.UiStateMessage;
import com.clover.remote.message.VaultCardMessage;
import com.clover.remote.message.VaultCardResponseMessage;
import com.clover.remote.message.VerifySignatureMessage;
import com.clover.remote.message.VoidPaymentMessage;
import com.clover.remote.message.WelcomeMessage;
import com.clover.remote.order.DisplayOrder;
import com.clover.remote.order.operation.DiscountsAddedOperation;
import com.clover.remote.order.operation.DiscountsDeletedOperation;
import com.clover.remote.order.operation.LineItemsAddedOperation;
import com.clover.remote.order.operation.LineItemsDeletedOperation;
import com.clover.remote.order.operation.OrderDeletedOperation;
import com.clover.sdk.internal.PayIntent;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.order.VoidReason;
import com.clover.sdk.v3.payments.Payment;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;

import java.util.List;

public class DefaultCloverDevice extends CloverDevice implements CloverTransportObserver {
  private static final String TAG = DefaultCloverDevice.class.getName();
  Gson gson = new Gson();
  private static int id = 0;
  private RefundResponseMessage refRespMsg;


  public DefaultCloverDevice(CloverDeviceConfiguration configuration) {
    this(configuration.getMessagePackageName(), configuration.getCloverTransport());
  }

  public DefaultCloverDevice(String packageName, CloverTransport transport) {
    super(packageName, transport);
    transport.Subscribe(this);
  }

  public void onDeviceConnected(CloverTransport transport) {
    notifyObserversConnected(transport);
  }


  public void onDeviceDisconnected(CloverTransport transport) {
    notifyObserversDisconnected(transport);
  }


  public void onDeviceReady(CloverTransport transport) {
    // now that the device is ready, let's send it a discovery request. the discovery response should trigger
    // the callback for the device observer that it is connected and able to communicate
    Log.d(getClass().getSimpleName(), "Sending Discovery Request");
    doDiscoveryRequest();
  }


  public void onMessage(String message) {
    try {
      RemoteMessage rMessage = gson.fromJson(message, RemoteMessage.class);

      Method m = null;

      try {

        if (!"ACK".equals(rMessage.method)) {
          m = Method.valueOf(rMessage.method);
          switch (m) {
            case BREAK:
              break;
            case CASHBACK_SELECTED:
              CashbackSelectedMessage cbsMessage = (CashbackSelectedMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversCashbackSelected(cbsMessage);
              break;
            case DISCOVERY_RESPONSE:
              Log.d(getClass().getSimpleName(), "Got a Discovery Response");
              DiscoveryResponseMessage drm = (DiscoveryResponseMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversReady(transport, drm);
              break;
            case FINISH_CANCEL:
              notifyObserversFinishCancel();
              break;
            case FINISH_OK:
              FinishOkMessage fokmsg = (FinishOkMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversFinishOk(fokmsg);
              break;
            case KEY_PRESS:
              KeyPressMessage kpm = (KeyPressMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversKeyPressed(kpm);
              break;
            case ORDER_ACTION_RESPONSE:
              break;
            case PARTIAL_AUTH:
              PartialAuthMessage partialAuth = (PartialAuthMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversPartialAuth(partialAuth);
              break;
            case PAYMENT_VOIDED:
              VoidPaymentMessage vpMessage = (VoidPaymentMessage) Message.fromJsonString(rMessage.payload);
              //Payment payment = gson.fromJson(vpMessage.payment, Payment.class);
              notifyObserversPaymentVoided(vpMessage.payment, vpMessage.voidReason);
              break;
            case TIP_ADDED:
              TipAddedMessage tipMessage = (TipAddedMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversTipAdded(tipMessage);
              break;
            case TX_START_RESPONSE:
              break;
            case TX_STATE:
              TxStateMessage txStateMsg = (TxStateMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversTxState(txStateMsg);
              break;
            case UI_STATE:
              UiStateMessage uiStateMsg = (UiStateMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversUiState(uiStateMsg);
              break;
            case VERIFY_SIGNATURE:
              VerifySignatureMessage vsigMsg = (VerifySignatureMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversVerifySignature(vsigMsg);
              break;
            case REFUND_RESPONSE:
              // for now, deprecating and refund is handled in finish_ok
              // finish_ok also get this message after a receipt, but it doesn't have all the information
              refRespMsg = (RefundResponseMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversPaymentRefundResponse(refRespMsg);
              break;
            case REFUND_REQUEST:
              //Outbound no-op
              break;
            case TIP_ADJUST_RESPONSE:
              TipAdjustResponseMessage tipAdjustMsg = (TipAdjustResponseMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversTipAdjusted(tipAdjustMsg);
              break;
            case VAULT_CARD_RESPONSE:
              VaultCardResponseMessage vcrm = (VaultCardResponseMessage) Message.fromJsonString(rMessage.payload);
              notifyObserverVaultCardResponse(vcrm);
              break;
            case CAPTURE_PREAUTH_RESPONSE:
              CapturePreAuthResponseMessage cparm = (CapturePreAuthResponseMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversCapturePreAuth(cparm);
            case CLOSEOUT_RESPONSE:
              CloseoutResponseMessage crm = (CloseoutResponseMessage) Message.fromJsonString(rMessage.payload);
              notifyObserversCloseout(crm);
            case DISCOVERY_REQUEST:
              //Outbound no-op
              break;
            case ORDER_ACTION_ADD_DISCOUNT:
              //Outbound no-op
              break;
            case ORDER_ACTION_ADD_LINE_ITEM:
              //Outbound no-op
              break;
            case ORDER_ACTION_REMOVE_LINE_ITEM:
              //Outbound no-op
              break;
            case ORDER_ACTION_REMOVE_DISCOUNT:
              //Outbound no-op
              break;
            case PRINT_CREDIT:
              //Outbound no-op
              break;
            case PRINT_CREDIT_DECLINE:
              //Outbound no-op
              break;
            case PRINT_IMAGE:
              //Outbound no-op
              break;
            case PRINT_PAYMENT:
              //Outbound no-op
              break;
            case PRINT_PAYMENT_DECLINE:
              //Outbound no-op
              break;
            case PRINT_PAYMENT_MERCHANT_COPY:
              //Outbound no-op
              break;
            case PRINT_TEXT:
              //Outbound no-op
              break;
            case SHOW_ORDER_SCREEN:
              //Outbound no-op
              break;
            case SHOW_THANK_YOU_SCREEN:
              //Outbound no-op
              break;
            case SHOW_WELCOME_SCREEN:
              //Outbound no-op
              break;
            case SIGNATURE_VERIFIED:
              //Outbound no-op
              break;
            case TERMINAL_MESSAGE:
              //Outbound no-op
              break;
            case TX_START:
              //Outbound no-op
              break;
            case VOID_PAYMENT:
              //Outbound no-op
              break;
            case CAPTURE_PREAUTH:
              //Outbound no-op
              break;
            case LAST_MSG_REQUEST:
              //Outbound no-op
              break;
            case LAST_MSG_RESPONSE:
              //Outbound no-op
              break;
            case TIP_ADJUST:
              //Outbound no-op
              break;
            case OPEN_CASH_DRAWER:
              //Outbound no-op
              break;
            case SHOW_PAYMENT_RECEIPT_OPTIONS:
              //Outbound no-op
              break;
//            case SHOW_REFUND_RECEIPT_OPTIONS:
//              //Outbound no-op
//              break;
//            case SHOW_MANUAL_REFUND_RECEIPT_OPTIONS:
//              //Outbound no-op
//              break;
            case REFUND_PRINT_PAYMENT:
              //Outbound no-op
              break;
            case VAULT_CARD:
              //Outbound no-op
              break;
            case CLOSEOUT_REQUEST:
              //Outbound no-op
              break;
          }
        }
      } catch (Exception e) {
        Log.e(TAG, "Invalid method type: " + rMessage.payload);
        e.printStackTrace();
      }

    } catch (Exception e) {
      e.printStackTrace();
      //onError(e);
    }
  }


  private void notifyObserversConnected(final CloverTransport transport) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (final CloverDeviceObserver observer : deviceObservers) {
          observer.onDeviceConnected(DefaultCloverDevice.this);
        }
        return null;
      }
    }.execute();
  }

  private void notifyObserversDisconnected(final CloverTransport transport) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (final CloverDeviceObserver observer : deviceObservers) {
          observer.onDeviceDisconnected(DefaultCloverDevice.this);
        }
        return null;
      }
    }.execute();
  }

  private void notifyObserversReady(final CloverTransport transport, final DiscoveryResponseMessage drm) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (final CloverDeviceObserver observer : deviceObservers) {
          observer.onDeviceReady(DefaultCloverDevice.this, drm);
        }
        return null;
      }
    }.execute();
  }

  //---------------------------------------------------
  /// <summary>
  /// this is for a payment refund
  /// </summary>
  /// <param name="rrm"></param>
  public void notifyObserversPaymentRefundResponse(final RefundResponseMessage rrm) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (final CloverDeviceObserver observer : deviceObservers) {
          observer.onPaymentRefundResponse(rrm.orderId, rrm.paymentId, rrm.refund, rrm.code);
        }
        return null;
      }
    }.execute();
  }

  public void notifyObserversKeyPressed(final KeyPressMessage keyPress) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (final CloverDeviceObserver observer : deviceObservers) {
          observer.onKeyPressed(keyPress.keyPress);
        }
        return null;
      }
    }.execute();
  }

  public void notifyObserversCashbackSelected(final CashbackSelectedMessage cbSelected) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (final CloverDeviceObserver observer : deviceObservers) {
          observer.onCashbackSelected(cbSelected.cashbackAmount);
        }
        return null;
      }
    }.execute();
  }

  public void notifyObserversTipAdded(final TipAddedMessage tipAdded) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onTipAdded(tipAdded.tipAmount);
        }
        return null;
      }
    }.execute();

  }

  public void notifyObserversTipAdjusted(final TipAdjustResponseMessage tarm) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onAuthTipAdjusted(tarm.paymentId, tarm.amount, tarm.success);
        }
        return null;
      }
    }.execute();

  }

  public void notifyObserversPartialAuth(final PartialAuthMessage partialAuth) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onPartialAuth(partialAuth.partialAuthAmount);
        }
        return null;
      }
    }.execute();

  }

  public void notifyObserversPaymentVoided(final Payment payment, final VoidReason reason) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onPaymentVoided(payment, reason);
        }
        return null;
      }
    }.execute();

  }

  public void notifyObserversVerifySignature(final VerifySignatureMessage verifySigMsg) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onVerifySignature(verifySigMsg.payment, verifySigMsg.signature);
        }
        return null;
      }
    }.execute();

  }

  public void notifyObserverVaultCardResponse(final VaultCardResponseMessage vaultCardResponseMessage) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onVaultCardResponse(vaultCardResponseMessage.card, vaultCardResponseMessage.status.toString(), vaultCardResponseMessage.reason);
        }
        return null;
      }
    }.execute();
  }

  public void notifyObserversUiState(final UiStateMessage uiStateMsg) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onUiState(uiStateMsg.uiState, uiStateMsg.uiText, uiStateMsg.uiDirection, uiStateMsg.inputOptions);
        }
        return null;
      }
    }.execute();
  }

  public void notifyObserversCapturePreAuth(final CapturePreAuthResponseMessage cparm) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onCapturePreAuth(cparm.status, cparm.reason, cparm.paymentId, cparm.amount, cparm.tipAmount);
        }
        return null;
      }
    }.execute();
  }

  public void notifyObserversCloseout(final CloseoutResponseMessage crm) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {

          observer.onCloseoutResponse(crm.status, crm.reason, crm.batch);
        }
        return null;
      }
    }.execute();
  }


  public void notifyObserversTxState(final TxStateMessage txStateMsg) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onTxState(txStateMsg.txState);
        }
        return null;
      }
    }.execute();

  }

  public void notifyObserversFinishCancel() {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          observer.onFinishCancel();
        }
        return null;
      }
    }.execute();

  }

  public void notifyObserversFinishOk(final FinishOkMessage msg) {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        for (CloverDeviceObserver observer : deviceObservers) {
          if (msg.payment != null) {
            observer.onFinishOk(msg.payment, msg.signature);
          } else if (msg.credit != null) {
            observer.onFinishOk(msg.credit);
          } else if (msg.refund != null) {
            observer.onFinishOk(msg.refund);
          }
        }
        return null;
      }
    }.execute();

  }

  public void doShowPaymentReceiptScreen(String orderId, String paymentId) {
    sendObjectMessage(new ShowPaymentReceiptOptionsMessage(orderId, paymentId, 2));
  }

//  public void doShowRefundReceiptScreen(String orderId, String refundId) {
//    sendObjectMessage(new ShowRefundReceiptOptionsMessage(orderId, refundId));
//  }

//  public void doShowManualRefundReceiptScreen(String orderId, String creditId) {
//    sendObjectMessage(new ShowManualRefundReceiptOptionsMessage(orderId, creditId));
//  }

  public void doKeyPress(KeyPress keyPress) {
    sendObjectMessage(new KeyPressMessage(keyPress));
  }

  public void doShowThankYouScreen() {
    sendObjectMessage(new ThankYouMessage());
  }

  public void doShowWelcomeScreen() {
    sendObjectMessage(new WelcomeMessage());
  }

  public void doSignatureVerified(Payment payment, boolean verified) {
    sendObjectMessage(new SignatureVerifiedMessage(payment, verified));
  }

  public void doTerminalMessage(String text) {
    sendObjectMessage(new TerminalMessage(text));
  }

  public void doOpenCashDrawer(String reason) {
    sendObjectMessage(new OpenCashDrawerMessage(reason) {
    }); // TODO: fix OpenCashDrawerMessage ctor
  }

  public void doCloseout(boolean allowOpenTabs, String batchId) {
    sendObjectMessage(new CloseoutRequestMessage(allowOpenTabs, batchId));
  }

  public void doTxStart(PayIntent payIntent, Order order, boolean suppressTipScreen) {
    sendObjectMessage(new TxStartRequestMessage(payIntent, order, suppressTipScreen));
  }

  public void doTipAdjustAuth(String orderId, String paymentId, long amount) {
    sendObjectMessage(new TipAdjustMessage(orderId, paymentId, amount));
  }

  public void doPrintText(List<String> textLines) {
    TextPrintMessage tpm = new TextPrintMessage(textLines);
    sendObjectMessage(tpm);
  }

  public void doPrintImage(Bitmap bitmap) {
    ImagePrintMessage ipm = new ImagePrintMessage(bitmap);
    sendObjectMessage(ipm);
  }


  public void doVoidPayment(final Payment payment, final VoidReason reason) {
    sendObjectMessage(new VoidPaymentMessage(payment, reason));

    // because we don't get a callback from the device, we can create one to keep the api consistent
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        notifyObserversPaymentVoided(payment, reason);
        return null;
      }
    }.execute();
  }

  public void doPaymentRefund(String orderId, String paymentId, long amount) {
    sendObjectMessage(new RefundRequestMessage(orderId, paymentId, amount, false));
  }

  public void doVaultCard(int cardEntryMethods) {
    sendObjectMessage(new VaultCardMessage(cardEntryMethods));
  }

  public void doCaptureAuth(String paymentId, long amount, long tipAmount) {
    sendObjectMessage(new CapturePreAuthMessage(paymentId, amount, tipAmount));
  }

  public void doDiscoveryRequest() {
    sendObjectMessage(new DiscoveryRequestMessage(false));
  }

  public void doOrderUpdate(DisplayOrder order, Object operation) {
    OrderUpdateMessage updateMessage = null;

    if (operation instanceof DiscountsAddedOperation) {
      updateMessage = new OrderUpdateMessage(order, (DiscountsAddedOperation) operation);
    } else if (operation instanceof DiscountsDeletedOperation) {
      updateMessage = new OrderUpdateMessage(order, (DiscountsDeletedOperation) operation);
    } else if (operation instanceof LineItemsAddedOperation) {
      updateMessage = new OrderUpdateMessage(order, (LineItemsAddedOperation) operation);
    } else if (operation instanceof LineItemsDeletedOperation) {
      updateMessage = new OrderUpdateMessage(order, (LineItemsDeletedOperation) operation);
    } else if (operation instanceof OrderDeletedOperation) {
      updateMessage = new OrderUpdateMessage(order, (OrderDeletedOperation) operation);
    } else {
      updateMessage = new OrderUpdateMessage(order);
    }

    sendObjectMessage(updateMessage);
  }

  @Override
  public void doResetDevice() {
    sendObjectMessage(new BreakMessage());
  }

  public void dispose() {
    deviceObservers.clear();
    refRespMsg = null;
    if (transport != null) {
      transport.dispose();
      transport = null;
    }
  }

  private void sendObjectMessage(Message message) {
    if (message == null) {
      Log.d(getClass().getName(), "Message is null");
      return;
    }
    Log.d(getClass().getName(), message.toString());
    if (message.method == null) {
      Log.e(getClass().getName(), "Invalid message", new IllegalArgumentException("Invalid message: " + message.toString()));
      return;
    }
    RemoteMessage remoteMessage = new RemoteMessage("" + id++, RemoteMessage.Type.COMMAND, this.packageName, message.method.toString(), message.toJsonString());

    String msg = gson.toJson(remoteMessage);
    transport.sendMessage(msg);
  }
}
