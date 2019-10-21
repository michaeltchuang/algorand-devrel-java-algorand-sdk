package com.algorand.algosdk.transaction;


import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.crypto.ParticipationPublicKey;
import com.algorand.algosdk.crypto.VRFPublicKey;
import com.algorand.algosdk.util.Digester;
import com.algorand.algosdk.util.Encoder;
import com.fasterxml.jackson.annotation.*;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * A raw serializable transaction class, used to generate transactions to broadcast to the network.
 * This is distinct from algod.model.Transaction, which is only returned for GET requests to algod.
 */
@JsonPropertyOrder(alphabetic=true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Transaction implements Serializable {
    private static final byte[] TX_SIGN_PREFIX = ("TX").getBytes(StandardCharsets.UTF_8);

    @JsonProperty("type")
    public Type type = Type.Default;

    // Instead of embedding POJOs and using JsonUnwrapped, we explicitly export inner fields. This circumvents our encoders'
    // inability to sort child fields.
    /* header fields ***********************************************************/
    @JsonProperty("snd")
    public Address sender = new Address();
    @JsonProperty("fee")
    public BigInteger fee = BigInteger.valueOf(0);
    @JsonProperty("fv")
    public BigInteger firstValid = BigInteger.valueOf(0);
    @JsonProperty("lv")
    public BigInteger lastValid = BigInteger.valueOf(0);
    @JsonProperty("note")
    public byte[] note;
    @JsonProperty("gen")
    public String genesisID = "";
    @JsonProperty("gh")
    public Digest genesisHash = new Digest();
    @JsonProperty("grp")
    public Digest group = new Digest();

    /* payment fields  *********************************************************/
    @JsonProperty("amt")
    public BigInteger amount = BigInteger.valueOf(0);
    @JsonProperty("rcv")
    public Address receiver = new Address();
    @JsonProperty("close")
    public Address closeRemainderTo = new Address(); // can be null, optional

    /* keyreg fields ***********************************************************/
    // VotePK is the participation public key used in key registration transactions
    @JsonProperty("votekey")
    public ParticipationPublicKey votePK = new ParticipationPublicKey();

    // selectionPK is the VRF public key used in key registration transactions
    @JsonProperty("selkey")
    public VRFPublicKey selectionPK = new VRFPublicKey();
    // voteFirst is the first round this keyreg tx is valid for
    @JsonProperty("votefst")
    public BigInteger voteFirst = BigInteger.valueOf(0);

    // voteLast is the last round this keyreg tx is valid for
    @JsonProperty("votelst")
    public BigInteger voteLast = BigInteger.valueOf(0);
    // voteKeyDilution
    @JsonProperty("votekd")
    public BigInteger voteKeyDilution = BigInteger.valueOf(0);
    
    /* asset creation and configuration fields *********************************/
    @JsonProperty("apar")
    public AssetParams assetParams = new AssetParams();
    @JsonProperty("caid")
    public AssetID assetID = new AssetID();

    /* asset transfer fields ***************************************************/
    @JsonProperty("xaid")
    public AssetID xferAsset = new AssetID();

    // The amount of asset to transfer. A zero amount transferred to self
    // allocates that asset in the account's Assets map.
    @JsonProperty("aamt")
    public BigInteger assetAmount = BigInteger.valueOf(0);

    // The sender of the transfer.  If this is not a zero value, the real
    // transaction sender must be the Clawback address from the AssetParams. If
    // this is the zero value, the asset is sent from the transaction's Sender.
    @JsonProperty("asnd")
    public Address assetSender = new Address();

    // The receiver of the transfer.
    @JsonProperty("arcv")
    public Address assetReceiver = new Address();

    // Indicates that the asset should be removed from the account's Assets map,
    // and specifies where the remaining asset holdings should be transferred.
    // It's always valid to transfer remaining asset holdings to the AssetID
    // account.
    @JsonProperty("aclose")
    public Address assetCloseTo = new Address();
    
    /* asset freeze fields */
    @JsonProperty("fadd")
    public Address freezeTarget = new Address();
    @JsonProperty("faid")
    public AssetID assetFreezeID = new AssetID();
    @JsonProperty("afrz")
    public boolean freezeState = false;
    
    /**
     * Create a payment transaction
     * @param fromAddr source address
     * @param toAddr destination address
     * @param fee transaction fee
     * @param amount payment amount
     * @param firstRound first valid round
     * @param lastRound last valid round
     */
    public Transaction(Address fromAddr, Address toAddr, BigInteger fee, BigInteger amount, BigInteger firstRound,
                       BigInteger lastRound) {
        this(fromAddr, fee, firstRound, lastRound, null, amount, toAddr, "", new Digest());
    }

    public Transaction(Address fromAddr, Address toAddr, BigInteger fee, BigInteger amount, BigInteger firstRound,
                       BigInteger lastRound,
                       String genesisID, Digest genesisHash) {
        this(fromAddr, fee, firstRound, lastRound, null, amount, toAddr, genesisID, genesisHash);
    }

    /**
     * Create a payment transaction. Make sure to sign with a suggested fee.
     * @param fromAddr source address
     * @param toAddr destination address
     * @param amount amount to send
     * @param firstRound first valid round
     * @param lastRound last valid round
     * @param genesisID genesis id
     * @param genesisHash genesis hash
     */
    public Transaction(Address fromAddr, Address toAddr, long amount, long firstRound, long lastRound,
                       String genesisID, Digest genesisHash) {
        this(fromAddr, BigInteger.valueOf(0), BigInteger.valueOf(firstRound), BigInteger.valueOf(lastRound), null, BigInteger.valueOf(amount), toAddr, genesisID, genesisHash);
    }

    public Transaction(Address sender, BigInteger fee, BigInteger firstValid, BigInteger lastValid, byte[] note,
                       BigInteger amount, Address receiver, String genesisID, Digest genesisHash) {
        this(sender, fee, firstValid, lastValid, note, genesisID, genesisHash, amount, receiver, new Address());
    }

    public Transaction(Address sender, BigInteger fee, BigInteger firstValid, BigInteger lastValid, byte[] note, String genesisID, Digest genesisHash,
                       BigInteger amount, Address receiver, Address closeRemainderTo) {
        this.type = Type.Payment;
        if (sender != null) this.sender = sender;
        if (fee != null) this.fee = fee;
        if (firstValid != null) this.firstValid = firstValid;
        if (lastValid != null) this.lastValid = lastValid;
        if (note != null) this.note = note;
        if (genesisID != null) this.genesisID = genesisID;
        if (genesisHash != null) this.genesisHash = genesisHash;
        if (amount != null) this.amount = amount;
        if (receiver != null) this.receiver = receiver;
        if (closeRemainderTo != null) this.closeRemainderTo = closeRemainderTo;
    }

    /**
     * Create a key registration transaction. No field can be null except the note field.
     * @param sender source address
     * @param fee transaction fee
     * @param firstValid first valid round
     * @param lastValid last valid round
     * @param note optional notes field (can be null)
     * @param votePK the new participation key to register
     * @param vrfPK the sortition key to register
     * @param voteFirst key reg valid first round
     * @param voteLast key reg valid last round
     * @param voteKeyDilution key reg dilution
     */
    public Transaction(Address sender, BigInteger fee, BigInteger firstValid, BigInteger lastValid, byte[] note,
                       String genesisID, Digest genesisHash,
                       ParticipationPublicKey votePK, VRFPublicKey vrfPK,
                       BigInteger voteFirst, BigInteger voteLast, BigInteger voteKeyDilution) {
        // populate with default values which will be ignored...
        this.type = Type.KeyRegistration;        
        if (sender != null) this.sender = sender;
        if (fee != null) this.fee = fee;
        if (firstValid != null) this.firstValid = firstValid;
        if (lastValid != null) this.lastValid = lastValid;
        if (note != null) this.note = note;
        if (genesisID != null) this.genesisID = genesisID;
        if (genesisHash != null) this.genesisHash = genesisHash;        
        if (votePK != null) this.votePK = votePK;
        if (vrfPK != null) this.selectionPK = vrfPK;
        if (voteFirst != null) this.voteFirst = voteFirst;
        if (voteLast != null) this.voteLast = voteLast;
        if (voteKeyDilution != null) this.voteKeyDilution = voteKeyDilution;
    }

    /**
     * Create an asset creation transaction. Note can be null. manager, reserve, freeze, and clawback can be zeroed.
     * @param sender source address
     * @param fee transaction fee
     * @param firstValid first valid round
     * @param lastValid last valid round
     * @param note optional note field (can be null)
     * @param genesisID
     * @param genesisHash
     * @param assetTotal total asset issuance
     * @param defaultFrozen whether accounts have this asset frozen by default
     * @param assetUnitName name of unit of the asset
     * @param assetName name of the asset
     * @param manager account which can reconfigure the asset
     * @param reserve account whose asset holdings count as non-minted
     * @param freeze account which can freeze or unfreeze holder accounts
     * @param clawback account which can issue clawbacks against holder accounts
     */
    public Transaction(Address sender, BigInteger fee, BigInteger firstValid, BigInteger lastValid, byte[] note,
                       String genesisID, Digest genesisHash, BigInteger assetTotal, boolean defaultFrozen,
                       String assetUnitName, String assetName, Address manager, Address reserve, Address freeze, Address clawback) {
        this.type = Type.AssetConfig;
        if (sender != null) this.sender = sender;
        if (fee != null) this.fee = fee;
        if (firstValid != null) this.firstValid = firstValid;
        if (lastValid != null) this.lastValid = lastValid;
        if (note != null) this.note = note;
        if (genesisID != null) this.genesisID = genesisID;
        if (genesisHash != null) this.genesisHash = genesisHash;
 
        this.assetParams = new AssetParams(assetTotal, defaultFrozen, assetUnitName, assetName, manager, reserve, freeze, clawback);        
    }

    /**
     * Create an asset configuration transaction. Note can be null. manager, reserve, freeze, and clawback can be zeroed.
     * @param sender source address
     * @param fee transaction fee
     * @param firstValid first valid round
     * @param lastValid last valid round
     * @param note optional note field (can be null)
     * @param genesisID
     * @param genesisHash
     * @param creator asset creator
     * @param index asset index
     * @param assetUnitName name of unit of the asset
     * @param assetName name of the asset
     * @param manager account which can reconfigure the asset
     * @param reserve account whose asset holdings count as non-minted
     * @param freeze account which can freeze or unfreeze holder accounts
     * @param clawback account which can issue clawbacks against holder accounts
     */
    public Transaction(Address sender, BigInteger fee, BigInteger firstValid, BigInteger lastValid, byte[] note,
                       String genesisID, Digest genesisHash, Address creator, BigInteger index,
                    Address manager, Address reserve, Address freeze, Address clawback) {
 
        this.type = Type.AssetConfig;
        if (sender != null) this.sender = sender;
        if (fee != null) this.fee = fee;
        if (firstValid != null) this.firstValid = firstValid;
        if (lastValid != null) this.lastValid = lastValid;
        if (note != null) this.note = note;
        if (genesisID != null) this.genesisID = genesisID;
        if (genesisHash != null) this.genesisHash = genesisHash;
        this.assetParams = new AssetParams(BigInteger.valueOf(0), false, "", "", manager, reserve, freeze, clawback);
        assetID = new AssetID(creator, index);
    }        

    public Transaction(Address sender, BigInteger fee, BigInteger firstValid, BigInteger lastValid, byte[] note,
                       String genesisID, Digest genesisHash, AssetID assetID, AssetParams assetParams) {
        this.type = Type.AssetConfig;
        if (sender != null) this.sender = sender;
        if (fee != null) this.fee = fee;
        if (firstValid != null) this.firstValid = firstValid;
        if (lastValid != null) this.lastValid = lastValid;
        if (note != null) this.note = note;
        if (genesisID != null) this.genesisID = genesisID;
        if (genesisHash != null) this.genesisHash = genesisHash;
        if (assetParams != null) this.assetParams = assetParams;
        if (assetID != null) this.assetID = assetID;
    }

    /**
     * Create an asset freeze/un-freeze transaction.
     * @param sender source address (must be the freeze manager for said asset)
     * @param fee transaction fee
     * @param firstValid first round transaction is valid
     * @param lastValid last round transaction is valid
     * @param note optional note field (can be null)
     * @param genesisID
     * @param genesisHash
     * @param assetFreezeID asset creator and index
     * @param freezeTarget account to be un-frozen or frozen
     * @param freezeState new frozen state for target account (true==frozen==cannot transact)
     */
    public Transaction (Address sender, BigInteger fee, BigInteger firstValid, BigInteger lastValid, byte[] note,
                        String genesisID, Digest genesisHash, AssetID assetFreezeID, Address freezeTarget, boolean freezeState) {
        this.type = Type.AssetFreeze;
        if (sender != null) this.sender = sender;
        if (fee != null) this.fee = fee;
        if (firstValid != null) this.firstValid = firstValid;
        if (lastValid != null) this.lastValid = lastValid;
        if (note != null) this.note = note;
        if (genesisID != null) this.genesisID = genesisID;
        if (genesisHash != null) this.genesisHash = genesisHash;
        if (freezeTarget != null) this.freezeTarget = freezeTarget;
        if (assetFreezeID != null) this.assetFreezeID = assetFreezeID;
        this.freezeState = freezeState;
    }
    
    // workaround for nested JsonValue classes
    @JsonCreator
    private Transaction(@JsonProperty("type") Type type,
                        //header fields
                        @JsonProperty("snd") byte[] sender,
                        @JsonProperty("fee") BigInteger fee,
                        @JsonProperty("fv") BigInteger firstValid,
                        @JsonProperty("lv") BigInteger lastValid,
                        @JsonProperty("note") byte[] note,
                        @JsonProperty("gen") String genesisID,
                        @JsonProperty("gh") byte[] genesisHash,
                        @JsonProperty("grp") byte[] group,			
                        // payment fields
                        @JsonProperty("amt") BigInteger amount,
                        @JsonProperty("rcv") byte[] receiver,
                        @JsonProperty("close") byte[] closeRemainderTo,
                        // keyreg fields
                        @JsonProperty("votekey") byte[] votePK,
                        @JsonProperty("selkey") byte[] vrfPK,
                        @JsonProperty("votefst") BigInteger voteFirst,
                        @JsonProperty("votelst") BigInteger voteLast,
                        @JsonProperty("votekd") BigInteger voteKeyDilution,
                        // asset creation and configuration
                        @JsonProperty("apar") AssetParams assetParams,
                        @JsonProperty("caid") AssetID assetID,
                        // Asset xfer transaction fields
                        @JsonProperty("xaid") AssetID xferAsset,
                        @JsonProperty("aamt") BigInteger assetAmount,
                        @JsonProperty("asnd") byte[] assetSender,
                        @JsonProperty("arcv") byte[] assetReceiver,
                        @JsonProperty("aclose") byte[] assetCloseTo,
                        // asset freeze fields
                        @JsonProperty("fadd") byte[] freezeTarget,
                        @JsonProperty("faid") AssetID assetFreezeID,
                        @JsonProperty("afrz") boolean freezeState) {
        this(
	     type,
	     //header fields
	     new Address(sender),
	     fee,
	     firstValid,
	     lastValid,
	     note,
	     genesisID,
	     new Digest(genesisHash),
	     new Digest(group),
	     // payment fields
	     amount,
	     new Address(receiver),
	     new Address(closeRemainderTo),
	     // keyreg fields
	     new ParticipationPublicKey(votePK),
	     new VRFPublicKey(vrfPK),
	     voteFirst,
	     voteLast,
	     voteKeyDilution,
	     // asset creation and configuration
	     assetParams,
	     assetID,
	     // asset transfer fields
	     xferAsset,
	     assetAmount,
	     new Address(assetSender),
	     new Address(assetReceiver),
	     new Address(assetCloseTo),
	     new Address(freezeTarget),
	     assetFreezeID,
	     freezeState);
    }

    /**
     * This is the private constructor which takes all the fields of Transaction
     **/
    private Transaction(
                        Type type,
                        //header fields
                        Address sender,
                        BigInteger fee,
                        BigInteger firstValid,
                        BigInteger lastValid,
                        byte[] note,
                        String genesisID,
                        Digest genesisHash,
                        Digest group,
                        // payment fields
                        BigInteger amount,
                        Address receiver,
                        Address closeRemainderTo,
                        // keyreg fields
                        ParticipationPublicKey votePK,
                        VRFPublicKey vrfPK,
                        BigInteger voteFirst,
                        BigInteger voteLast,
                        // voteKeyDilution
                        BigInteger voteKeyDilution,
                        // asset creation and configuration
                        AssetParams assetParams,
                        AssetID assetID,
                        // asset transfer fields
                        AssetID xferAsset,
                        BigInteger assetAmount,
                        Address assetSender,
                        Address assetReceiver,
                        Address assetCloseTo,
                        Address freezeTarget,
                        AssetID assetFreezeID,
                        boolean freezeState) {
        if (type != null) this.type = type;
        if (sender != null) this.sender = sender;
        if (fee != null) this.fee = fee;
        if (firstValid != null) this.firstValid = firstValid;
        if (lastValid != null) this.lastValid = lastValid;
        if (note != null) this.note = note;
        if (genesisID != null) this.genesisID = genesisID;
        if (genesisHash != null) this.genesisHash = genesisHash;
        if (group != null) this.group = group;	
        if (amount != null) this.amount = amount;
        if (receiver != null) this.receiver = receiver;
        if (closeRemainderTo != null) this.closeRemainderTo = closeRemainderTo;
        if (votePK != null) this.votePK = votePK;
        if (vrfPK != null) this.selectionPK = vrfPK;
        if (voteFirst != null) this.voteFirst = voteFirst;
        if (voteLast != null) this.voteLast = voteLast;
        if (voteKeyDilution != null) this.voteKeyDilution = voteKeyDilution;
        if (assetParams != null) this.assetParams = assetParams;
        if (assetID != null) this.assetID = assetID;
        if (xferAsset != null) this.xferAsset = xferAsset;
        if (assetAmount != null) this.assetAmount = assetAmount;
        if (assetSender != null) this.assetSender = assetSender;
        if (assetReceiver != null) this.assetReceiver = assetReceiver;
        if (assetCloseTo != null) this.assetCloseTo = assetCloseTo;
        if (freezeTarget != null) this.freezeTarget = freezeTarget;
        if (assetFreezeID != null) this.assetFreezeID = assetFreezeID;
        this.freezeState = freezeState;
    }

    public Transaction() {}


    /**
     * Base constructor with flat fee for asset xfer transactions.
     * @param flatFee is the transaction flat fee
     * @param firstRound is the first round this txn is valid (txn semantics
     * unrelated to asset management)
     * @param lastRound is the last round this txn is valid
     * @param note
     * @param genesisID corresponds to the id of the network
     * @param genesisHash corresponds to the base64-encoded hash of the genesis
     * of the network
     * @param assetCreator is the address of the asset creator
     * @param assetIndex is the asset index
     **/
    private Transaction(// Asset xfer transaction
            BigInteger flatFee,
            BigInteger firstRound,
            BigInteger lastRound,
            byte [] note,
            String genesisID,
            Digest genesisHash,
            Address assetCreator,
            BigInteger assetIndex) {

        this.type = Type.AssetTransfer;
        if (flatFee != null) this.fee = flatFee;
        if (firstRound != null) this.firstValid = firstRound;
        if (lastRound != null) this.lastValid = lastRound;
        if (note != null) this.note = note;
        if (genesisID != null) this.genesisID = genesisID;
        if (genesisHash != null) this.genesisHash = genesisHash;
        if (assetCreator != null) this.xferAsset = new AssetID(assetCreator,
                assetIndex);

    }

    /**
     * Creates a tx to mark the account as willing to accept the asset.
     * @param acceptingAccount is a checksummed, human-readable address that
     * will accept receiving the asset.
     * @param flatFee is the transaction flat fee
     * @param firstRound is the first round this txn is valid (txn semantics
     * unrelated to asset management)
     * @param lastRound is the last round this txn is valid
     * @param note
     * @param genesisID corresponds to the id of the network
     * @param genesisHash corresponds to the base64-encoded hash of the genesis
     * of the network
     * @param assetCreator is the address of the asset creator
     * @param assetIndex is the asset index
     **/
    public static Transaction createAcceptAssetTransaction( //AssetTransaction
            Address acceptingAccount, 
            BigInteger flatFee,
            BigInteger firstRound,
            BigInteger lastRound,
            byte [] note,
            String genesisID,
            Digest genesisHash,
            Address assetCreator,
            BigInteger assetIndex) {

        Transaction tx = createTransferAssetTransaction(
                acceptingAccount,
                acceptingAccount,
                new Address(),
                BigInteger.valueOf(0),
                flatFee,
                firstRound,
                lastRound,
                note,
                genesisID,
                genesisHash,
                assetCreator,
                assetIndex);

        return tx;
    }

    /**
     * Creates a tx for sending some asset from an asset holder to another user.
     *  The asset receiver must have marked itself as willing to accept the
     *  asset.
     * @param assetSender is a checksummed, human-readable address that will
     * send the transaction and assets
     * @param assetReceiver is a checksummed, human-readable address what will
     * receive the assets
     * @param assetCloseTo is a checksummed, human-readable address that
     * behaves as a close-to address for the asset transaction; the remaining
     * assets not sent to assetReceiver will be sent to assetCloseTo. Leave
     * blank for no close-to behavior.
     * @param assetAmount is the number of assets to send
     * @param flatFee is the transaction flat fee
     * @param firstRound is the first round this txn is valid (txn semantics
     * unrelated to asset management)
     * @param lastRound is the last round this txn is valid
     * @param note
     * @param genesisID corresponds to the id of the network
     * @param genesisHash corresponds to the base64-encoded hash of the genesis
     * of the network
     * @param assetCreator is the address of the asset creator
     * @param assetIndex is the asset index
     **/
    public static Transaction createTransferAssetTransaction(// AssetTransaction
            Address assetSender,
            Address assetReceiver,
            Address assetCloseTo,
            BigInteger assetAmount,
            BigInteger flatFee,
            BigInteger firstRound,
            BigInteger lastRound,
            byte [] note,
            String genesisID,
            Digest genesisHash,
            Address assetCreator,
            BigInteger assetIndex) {

        Transaction tx = new Transaction(
                flatFee,    // fee
                firstRound, // fv
                lastRound, // lv
                note, //note
                genesisID, // gen
                genesisHash, // gh
                assetCreator, // c
                assetIndex); // i

        tx.assetReceiver = assetReceiver; //arcv
        tx.assetCloseTo = assetCloseTo; // aclose
        tx.assetAmount = assetAmount; // aamt
        tx.sender = assetSender; // snd
        return tx;
    }







    
    /**
     * TxType represents a transaction type.
     */
    public enum Type {
        Default(""),
        Payment("pay"),
        KeyRegistration("keyreg"),
        AssetConfig("acfg"),
        AssetTransfer("axfer"),
        AssetFreeze("afrz");

        private final String value;
        private Type(String value) {
            this.value = value;
        }

        /**
         * Get underlying string value
         * @return String the string repr of this txtype
         */
        @JsonValue
        public String getValue() {
            return this.value;
        }
    }

    /**
     * Return encoded representation of the transaction
     */
    public byte[] bytes() throws IOException {
        try {
            return Encoder.encodeToMsgPack(this);
        } catch (IOException e) {
            throw new RuntimeException("serialization failed", e);
        }
    }

    /**
     * Return encoded representation of the transaction with a prefix
     * suitable for signing
     */
    public byte[] bytesToSign() throws IOException {
        try {
            byte[] encodedTx = Encoder.encodeToMsgPack(this);
            byte[] prefixEncodedTx = new byte[encodedTx.length + TX_SIGN_PREFIX.length];
            System.arraycopy(TX_SIGN_PREFIX, 0, prefixEncodedTx, 0, TX_SIGN_PREFIX.length);
            System.arraycopy(encodedTx, 0, prefixEncodedTx, TX_SIGN_PREFIX.length, encodedTx.length);
            return prefixEncodedTx;
        } catch (IOException e) {
            throw new RuntimeException("serialization failed", e);
        }
    }

    /**
     * Return transaction ID as Digest
     */
    public Digest rawTxID() throws IOException {
        try {
         return new Digest(Digester.digest(this.bytesToSign()));
        } catch (IOException e) {
            throw new RuntimeException("tx computation failed", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("tx computation failed", e);
        }
    }

    /**
     * Return transaction ID as string
     */
    public String txID() throws IOException, NoSuchAlgorithmException {
        return Encoder.encodeToBase32StripPad(this.rawTxID().getBytes());
    }

    public void assignGroupID(Digest gid) {
        this.group = gid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return type == that.type &&
                sender.equals(that.sender) &&
                fee.equals(that.fee) &&
                firstValid.equals(that.firstValid) &&
                lastValid.equals(that.lastValid) &&
                Arrays.equals(note, that.note) &&
                genesisID.equals(that.genesisID) &&
                genesisHash.equals(that.genesisHash) &&
                group.equals(that.group) &&
                amount.equals(that.amount) &&
                receiver.equals(that.receiver) &&
                closeRemainderTo.equals(that.closeRemainderTo) &&
                votePK.equals(that.votePK) &&
                selectionPK.equals(that.selectionPK) &&
                voteFirst.equals(that.voteFirst) &&
                voteLast.equals(that.voteLast) &&
                voteKeyDilution.equals(that.voteKeyDilution) &&
                assetParams.equals(that.assetParams) &&
                assetID.equals(that.assetID) &&
	        xferAsset.equals(that.xferAsset) &&
       	        assetAmount.equals(that.assetAmount) &&
	        assetSender.equals(that.assetSender) &&
	        assetReceiver.equals(that.assetReceiver) &&
	        assetCloseTo.equals(that.assetCloseTo) &&
                freezeTarget.equals(that.freezeTarget) &&
                assetFreezeID.equals(that.assetFreezeID) &&
                freezeState == that.freezeState;
    }

    @JsonPropertyOrder(alphabetic=true)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class AssetParams implements Serializable {
        // total asset issuance
        @JsonProperty("t")
        public BigInteger assetTotal = BigInteger.valueOf(0);
        // whether each account has their asset slot frozen for this asset by default
        @JsonProperty("df")
        public boolean assetDefaultFrozen = false;
        // a hint to the unit name of the asset
        @JsonProperty("un")
        public String assetUnitName = "";
        // the name of the asset
        @JsonProperty("an")
        public String assetName = "";
        // the address which has the ability to reconfigure the asset
        @JsonProperty("m")
        public Address assetManager = new Address();
        // the asset reserve: assets owned by this address do not count against circulation
        @JsonProperty("r")
        public Address assetReserve = new Address();
        // the address which has the ability to freeze/unfreeze accounts holding this asset
        @JsonProperty("f")
        public Address assetFreeze = new Address();
        // the address which has the ability to issue clawbacks against asset-holding accounts
        @JsonProperty("c")
        public Address assetClawback = new Address();
        
        public AssetParams(BigInteger assetTotal, boolean defaultFrozen, String assetUnitName, String assetName, Address manager, Address reserve, Address freeze, Address clawback) {
            if(assetTotal != null) this.assetTotal = assetTotal;
            this.assetDefaultFrozen = defaultFrozen;
            if(assetUnitName != null) this.assetUnitName = assetUnitName;
            if(assetName != null) this.assetName = assetName;
            if(manager != null) this.assetManager = manager;
            if(reserve != null) this.assetReserve = reserve;
            if(freeze != null) this.assetFreeze = freeze;
            if(clawback != null) this.assetClawback = clawback;
        }

        public AssetParams() {

        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AssetParams that = (AssetParams) o;
            return assetTotal.equals(that.assetTotal) &&
                (assetDefaultFrozen == that.assetDefaultFrozen) &&
                assetName.equals(that.assetName) &&
                assetUnitName.equals(that.assetUnitName) &&
                assetManager.equals(that.assetManager) &&
                assetReserve.equals(that.assetReserve) &&
                assetFreeze.equals(that.assetFreeze) &&
                assetClawback.equals(that.assetClawback);
        }

        @JsonCreator
        private AssetParams(@JsonProperty("t") BigInteger assetTotal,
            @JsonProperty("df") boolean assetDefaultFrozen,
            @JsonProperty("un") String assetUnitName,
            @JsonProperty("an") String assetName,
            @JsonProperty("m") byte[] assetManager,
            @JsonProperty("r") byte[] assetReserve,
            @JsonProperty("f") byte[] assetFreeze,
            @JsonProperty("c") byte[] assetClawback) {
            this(assetTotal, assetDefaultFrozen, assetUnitName, assetName, new Address(assetManager), new Address(assetReserve), new Address(assetFreeze), new Address(assetClawback));
        }
    }

    @JsonPropertyOrder(alphabetic=true)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class AssetID implements Serializable {
        // asset creator
        @JsonProperty("c")
        public Address creator = new Address();
        // asset index
        @JsonProperty("i")
        public BigInteger index = BigInteger.valueOf(0);

        public AssetID(Address creator, BigInteger index) {
            if(creator != null) this.creator = creator;
            if(index != null) this.index = index;
        }

        public AssetID() {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AssetID that = (AssetID) o;
            return creator.equals(that.creator) &&
                index.equals(that.index);
        }

        @JsonCreator
        private AssetID(@JsonProperty("c") byte[] creator,
            @JsonProperty("i") BigInteger index) {
                this(new Address(creator), index);
        }
    }

}
