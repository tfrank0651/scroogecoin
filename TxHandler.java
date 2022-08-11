
import java.util.ArrayList;
import java.util.HashMap;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 * ----------------------------------------------------------------------
	 * Written by Tyler Frank
	 * UTSA ID: zaf455
	 * CS 6393 Lab 2
	 * 11/2/2021
	 */
	
	private final UTXOPool utxoPool;
	
	public TxHandler(UTXOPool utxoPool) {
		this.utxoPool = new UTXOPool(utxoPool);
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool,
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx's output values are non-negative, and
	 * (5) the sum of tx's input values is greater than or equal to the sum of   
	 *     its output values; and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {
		// get all Tx outputs
		ArrayList<Transaction.Output> allTxOutputs = tx.getOutputs();
		// get all Tx inputs
		ArrayList<Transaction.Input> allTxInputs = tx.getInputs();
		// Variables to track input/output sums
		double sumInputs = 0.0;
		double sumOutputs = 0.0;	
		// get all current UTXO, will use to check if already used
		ArrayList<UTXO> usedUTXO = utxoPool.getAllUTXO();
		// Using a hashmap <UTXO, Boolean> to track if UTXO was already claimed
		HashMap<UTXO, Boolean> checkIfUsed = new HashMap<UTXO, Boolean>();
		
		// set all UTXOs in pool to false (not seen)
		for (UTXO usedutxo : usedUTXO) {
			checkIfUsed.put(usedutxo, false);
		}
		
		// Check each input to see if it exists within the UTXO Pool by looping through all inputs in the Tx
		// used to track the index of the current input, needed for getRawDataToSign
		int inputIdx = 0;
		for (Transaction.Input in : allTxInputs) {
			// create new utxo to see if exists in pool with prevTx hash and outputIndex
			UTXO tempUTXO = new UTXO(in.prevTxHash, in.outputIndex);
			
			// * (1) all outputs claimed by Tx are in the current UTXO pool, 			
			if (!utxoPool.contains(tempUTXO)) {
				return false;
			}
		
			// (2) the signatures on each input of tx are valid,
			else {
				// input exists in the current utxo pool
				// need hash of prevTx + index of output from prevTx + signature
				// hash of prevTx = in.prevTxHash (in tempUTXO)
				// index of output from prevTx = in.outputIndex (in tempUTXO)
				// signature = in.signature

				// message digest getRawDataToSign(int index)
				byte[] mdData = tx.getRawDataToSign(inputIdx);
				Transaction.Output tempOutput = utxoPool.getTxOutput(tempUTXO);
				// sig := sign(privKey, message)
				// isvalid:= verify (pubKey, message, signature)
				if (!tempOutput.address.verifySignature(mdData, in.signature)) {
					return false;
				}
				// update input sum
				sumInputs += tempOutput.value;
			}
			
			// * (3) no UTXO is claimed multiple times by tx, 
			// Using a hashmap <UTXO, Boolean>, set value to true if used,
			// if the value has already been used, return false as the utxo was claimed multiple times
			if (!checkIfUsed.get(tempUTXO)) {
				checkIfUsed.replace(tempUTXO, true);
			}
			else {
				return false;
			}
			// update, moving to next input
			inputIdx++;
		}
		
		// Handle output logic (verify no output value is negative and input > output)	
		for (Transaction.Output op : allTxOutputs) {
			//update output sum
			sumOutputs += op.value;
			// * (4) all of tx's output values are non-negative, and
			if (op.value < 0) {
				return false;
			}
			// * (5) the sum of tx's input values is greater than or equal to the sum of   
	        // its output values;
			if (sumOutputs > sumInputs) {
				return false;
			}
		}

		// if all conditions have been satisfied, return true for valid tx
		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// validTx array to return
		Transaction[] validTxs = new Transaction[possibleTxs.length];
		int validIdx = 0;
		// loop through all possibleTxs
		for (Transaction tx : possibleTxs) {
			// get all input/output
			ArrayList<Transaction.Output> allTxOutputs = tx.getOutputs();
			ArrayList<Transaction.Input> allTxInputs = tx.getInputs();
			// check if valid
			if (isValidTx(tx)) {
				// remove utxo(s) from pool
				for (Transaction.Input in : allTxInputs) {
					// UTXO(byte[] txHash, int index)
					UTXO removeUTXO = new UTXO(in.prevTxHash, in.outputIndex);
					utxoPool.removeUTXO(removeUTXO);
				}
				// add new utxo(s) to pool
				int currIdx = 0; 
				for (Transaction.Output op : allTxOutputs) {	
					// public UTXO(byte[] txHash, int index)
					UTXO addUTXO = new UTXO(tx.getHash(), currIdx);
					currIdx++;
					utxoPool.addUTXO(addUTXO, op);
				}
				// update validTxs
				validTxs[validIdx] = tx;
				validIdx++;
			}
		}
		// clean up validTxs and return appropriate length list
		final Transaction[] returnTxs = new Transaction[validIdx];
		System.arraycopy(validTxs, 0, returnTxs, 0, validIdx);
		return returnTxs;
	}
} 
