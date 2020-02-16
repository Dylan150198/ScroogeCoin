import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	private UTXOPool utxoPool;

	public TxHandler(UTXOPool utxoPool) {
		this.utxoPool = new UTXOPool(utxoPool);

	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool, 
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
	        its output values;
	   and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {
		Set<UTXO> utxoHashSet = new HashSet<UTXO>();
		double inputTotal = 0;
		double outputTotal = 0;

		List<Transaction.Input> inputList = tx.getInputs();

		for (int i = 0; i < inputList.size(); i++){
			Transaction.Input input = inputList.get(i);

			// Check #1
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			if(!this.utxoPool.contains(utxo)){
				return false;
			}

			// Check #2
			Transaction.Output output = utxoPool.getTxOutput(utxo);

			if(!output.address.verifySignature(tx.getRawDataToSign(i),input.signature)){
				return false;
			}

			// Check #3
			if(!utxoHashSet.add(utxo)){
				return false;
			}

			inputTotal += output.value;
		}


		// Check #4
		for (int i = 0; i< tx.numOutputs(); i++) {
			Transaction.Output txOutput = tx.getOutput(i);
			outputTotal += txOutput.value;
			if(txOutput.value < 0 ){
				return false;
			}
		}

		// Check #5
		if(outputTotal > inputTotal){
			return false;
		}

		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		List<Transaction> transactionList = new ArrayList<>();
		for (int i = 0; i < possibleTxs.length; i++){
			Transaction transaction = possibleTxs[i];
			// Check if Transaction is valid, if yes, add it to the list
			if(isValidTx(transaction)){
				transactionList.add(transaction);

				// Balance UTXO Pools; REMOVE UTXO's from input pool and add to output pool.
				for (Transaction.Input in : transaction.getInputs()) {
					UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
					utxoPool.removeUTXO(utxo);
				}
				for (int j = 0; j < transaction.numOutputs(); j++) {
					Transaction.Output out = transaction.getOutput(j);
					UTXO utxo = new UTXO(transaction.getHash(), j);
					utxoPool.addUTXO(utxo, out);
				}
			}
		}

		// Return mutually valid array of accepted transactions
		Transaction[] result = new Transaction[transactionList.size()];
		transactionList.toArray(result);
		return result;
	}

} 
