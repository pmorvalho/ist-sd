package org.komparator.mediator.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jws.WebService;

import org.komparator.supplier.ws.BadProductId_Exception;
import org.komparator.supplier.ws.BadQuantity_Exception;
import org.komparator.supplier.ws.BadText_Exception;
import org.komparator.supplier.ws.InsufficientQuantity_Exception;
import org.komparator.supplier.ws.ProductView;
import org.komparator.supplier.ws.cli.SupplierClient;
import org.komparator.supplier.ws.cli.SupplierClientException;

import pt.ulisboa.tecnico.sdis.ws.cli.CreditCardClient;
import pt.ulisboa.tecnico.sdis.ws.cli.CreditCardClientException;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINamingException;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDIRecord;

@WebService(
		endpointInterface = "org.komparator.mediator.ws.MediatorPortType", 
		wsdlLocation = "mediator.wsdl", 
		name = "MediatorWebService", 
		portName = "MediatorPort", 
		targetNamespace = "http://ws.mediator.komparator.org/", 
		serviceName = "MediatorService"
)
public class MediatorPortImpl implements MediatorPortType{

	// end point manager
	private MediatorEndpointManager endpointManager;
	
	//shop list
	private ArrayList<ShoppingResultView> shoppingResults = new ArrayList<ShoppingResultView>();

	private List<CartView> carts = new ArrayList<CartView>();
	
	private static int NumberOfBoughtCarts;

	public MediatorPortImpl(MediatorEndpointManager endpointManager) {
		this.endpointManager = endpointManager;
	}

	// Main operations -------------------------------------------------------
	
	@Override
	public List<ItemView> getItems(String productId) throws InvalidItemId_Exception {
		// check input - description
		if (productId == null)
			throwInvalidItemId("Product identifier cannot be null!");
		productId = productId.trim();
		if (productId.length() == 0)
			throwInvalidItemId("Product identifier cannot be empty or whitespace!");
		if (!checkId(productId))
			throwInvalidItemId("Product identifier must be alphanumeric without spaces!");
		
		List<SupplierClient> supClientList = getSupplierClients(getSuppliers());
		
    	List<ItemView> itemList = new ArrayList<ItemView>();
    	for(SupplierClient client : supClientList){
    		try{
    			ProductView prod = client.getProduct(productId);
    			if (prod == null)
    				continue;
    			itemList.add(createItemView(prod, client));
    		}
    		catch(BadProductId_Exception e){
    			throwInvalidItemId("Invalid item ID, failed.");
    		}
    	}
    	
    	Collections.sort(itemList,new ItemPriceComparator() );
		return itemList;
	}
	
	@Override
	public List<ItemView> searchItems(String descText) throws InvalidText_Exception {
		// check input - description
		if (descText == null)
			throwInvalidText("Product description cannot be null!");
		descText = descText.trim();
		if (descText.length() == 0)
			throwInvalidText("Product description cannot be empty or whitespace!");
		
		List<ItemView> itemList = new ArrayList<ItemView>();
		
		for (SupplierClient supC : getSupplierClients(getSuppliers())) {
			try {
				for(ProductView prod : supC.searchProducts(descText)) {
					itemList.add(createItemView(prod, supC));
				}
			} catch (BadText_Exception e) {
				//Should not happen. Error handled before
				throwInvalidText("Invalid description");
			}
		}
		
		Collections.sort(itemList,new ItemNameComparator() );
		return itemList;
	}

	@Override
	public void addToCart(String cartId, ItemIdView itemId, int itemQty) throws InvalidCartId_Exception,
	InvalidItemId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {		
		
		if( cartId == null || !checkId(cartId.trim()) ) throwInvalidCartId("Cart ID is incorrect, failed.");
				
		if( itemId == null || !checkId(itemId.getProductId().trim()) || getItems(itemId.getProductId()).isEmpty())  throwInvalidItemId("Item ID is incorrect, failed.");
		
		if( itemQty <= 0 ) throwInvalidQuantity("Quantity is invalid, failed.");

		int supQuantity=0;
		List<SupplierClient> clientsList = new ArrayList<SupplierClient>();
		SupplierClient client = null;
		ProductView product = null;

		try {

			Collection<UDDIRecord> supplier = endpointManager.getUddiNaming().listRecords(itemId.getSupplierId());
			if(!(clientsList = getSupplierClients(supplier)).isEmpty()) client = clientsList.get(0);
			else throw new BadProductId_Exception(itemId.getSupplierId(), null);

			if (client == null ) throw new BadProductId_Exception(itemId.getSupplierId(), null);

			product = client.getProduct(itemId.getProductId());

			if (product == null ) throw new BadProductId_Exception(itemId.getProductId(), null);

			supQuantity = product.getQuantity();

		} catch (BadProductId_Exception e) {
			throwInvalidItemId("Item ID is incorrect, failed.");
		} catch (UDDINamingException e) {
			throwInvalidItemId("Item ID is incorrect, failed.");
		}

		if( supQuantity < itemQty) throwNotEnoughItems("Not enough items, failed.");
		synchronized(this){
			for(CartView c : carts){

				if(c.getCartId().equals(cartId)){

					for(CartItemView civ : c.getItems()){

						if(civ.getItem().getItemId().getProductId().equals(itemId.getProductId()) && 
								civ.getItem().getItemId().getSupplierId().equals(itemId.getSupplierId())){
							int qty = civ.getQuantity() + itemQty;
							if(qty > supQuantity) throwNotEnoughItems("Not enough items, failed.");
							civ.setQuantity(qty);
							return;
						}
					}

					c.getItems().add(createCartItemView(product, client, itemQty));
					return;
				}
			}

			carts.add(createCartView(cartId, product, client, itemQty));
		}
	}

	@Override
	public ShoppingResultView buyCart(String cartId, String creditCardNr)
			throws EmptyCart_Exception, InvalidCartId_Exception, InvalidCreditCard_Exception {
		
		if( cartId==null || !checkId(cartId.trim()) ) throwInvalidCartId("Cart ID is incorrect, failed.");
		
		CreditCardClient ccClient = getCreditCardClient(getCreditCard());
		if(!ccClient.validateNumber(creditCardNr)){
			throwInvalidCreditCard("Invalid Credit Card, could not validate number, failed");
		}
		//Still have to add price, result and define purchased and dropped items, since we don't know them yet
		//Set ID
		NumberOfBoughtCarts++;
		ShoppingResultView shoppingResult = createShoppingResultView("CartResult"+NumberOfBoughtCarts,null,0);
		List<CartItemView> allItems = new ArrayList<CartItemView>();
		int totalprice=0;
		boolean foundCart=false;
		synchronized(this){
			for(CartView c : carts){

				if(c.getCartId().equals(cartId)){
					foundCart=true;
					if(c.getItems().size()==0){
						throwEmptyCart("The selected cart is empty, failed.");
					}

					for(CartItemView civ : c.getItems()){
						allItems.add(civ);

						String productId = civ.getItem().getItemId().getProductId();
						String supplierId = civ.getItem().getItemId().getSupplierId();
						int quantity = civ.getQuantity();
						String supplier;
						SupplierClient client;
						try {
							supplier = endpointManager.getUddiNaming().lookup(supplierId);
						} catch (UDDINamingException e) {
							System.out.println("Could not find supplier, continuing");
							continue;
						}

						client= getSupplierClient(supplier);

						try {
							client.buyProduct(productId, quantity);
						} catch (BadProductId_Exception e) {
							System.out.println("Malformed product ID, continuing");
							continue;
						} catch (BadQuantity_Exception e) {
							System.out.println("Invalid quantity, continuing");
							continue;
						} catch (InsufficientQuantity_Exception e) {
							System.out.println("Insufficient quantity available, continuing");
							continue;
						}
						//Set purchased items
						shoppingResult.getPurchasedItems().add(civ);
						totalprice+= civ.getItem().getPrice()*civ.getQuantity();
					}
				}
			}

			if(!foundCart){
				throwInvalidCartId("Could not find cart, failed");
			}
			//set dropped items
			for(CartItemView civ : allItems){
				if(!shoppingResult.getPurchasedItems().contains(civ)){
					shoppingResult.getDroppedItems().add(civ);
				}
			}

			//set result
			if(shoppingResult.getPurchasedItems().isEmpty()){
				shoppingResult.setResult(Result.EMPTY);
			}
			else if(shoppingResult.getPurchasedItems().equals(allItems)){
				shoppingResult.setResult(Result.COMPLETE);
			}
			else{
				shoppingResult.setResult(Result.PARTIAL);
			}
			//Set price
			shoppingResult.setTotalPrice(totalprice);
			//Shopping result finished
			shoppingResults.add(shoppingResult);
			return shoppingResult;
		}
	}
	
    
	// Auxiliary operations --------------------------------------------------	
	
	@Override
    public String ping(String arg0){
    	
    	Collection<UDDIRecord> suppliers=getSuppliers();
    	List<SupplierClient> supClientList = getSupplierClients(suppliers);
    	String result = "";
    	for(SupplierClient client : supClientList){
    		result+=client.ping(arg0)+ "\n";
    	}
    	
    	return result;
    }

	@Override
	public void clear() {
		
		List<SupplierClient> supClientList = getSupplierClients(getSuppliers());
		for(SupplierClient client : supClientList){
			client.clear();
		}
		
		carts.clear();
		shoppingResults.clear();
		NumberOfBoughtCarts=0;
	}

	@Override
	public List<CartView> listCarts() {
		return carts;
	}
		

	@Override
	public List<ShoppingResultView> shopHistory() {
		return shoppingResults;
	}
	
	
	// General Helpers -------------------------------------------------------
	
	public String getCreditCard(){
		String cc;
    	
    	try{
    		cc = endpointManager.getUddiNaming().lookup("CreditCard");
    	}
    	catch(UDDINamingException e){
    		System.out.println("Could not find Credit Card");
    		return null;
    	}
    	
    	return cc;
	} 
	
	public CreditCardClient getCreditCardClient(String record){
    	CreditCardClient client;
    	try{
    		client = new CreditCardClient(record);
    	}
    	catch(CreditCardClientException e){
    		System.out.println("could not create credit card client");
    		return null;
    	}
    	
    	return client;
    	
	}
	
	public Collection<UDDIRecord> getSuppliers(){
		Collection<UDDIRecord> suppliers;
    	
    	try{
    		suppliers = endpointManager.getUddiNaming().listRecords("A68_Supplier%");
    	}
    	catch(UDDINamingException e){
    		System.out.println("Could not list suppliers");
    		return null;
    	}
    	return suppliers;
	} 
	
	public List<SupplierClient> getSupplierClients(Collection<UDDIRecord> suppliers){
    	List<SupplierClient> supClientList = new ArrayList<SupplierClient>();
    	for(UDDIRecord record : suppliers){
    		try{
    			SupplierClient client = new SupplierClient(record.getUrl());
    			client.setWsName(record.getOrgName());
    			supClientList.add(client);
    			
    		}
    		catch(SupplierClientException e){
    			System.out.println("Could not create supplier clients");
    			return null;
    		}
    	}
    	return supClientList;
    	
	}
	
	public SupplierClient getSupplierClient(String url){
    	SupplierClient client;
		try{
    		client = new SupplierClient(url);

    	}
    	catch(SupplierClientException e){
    		System.out.println("Could not create supplier clients");
    		return null;
    	}
    	
    	return client;
    	
	}

	class ItemPriceComparator implements Comparator<ItemView> {
	    @Override
	    public int compare(ItemView a, ItemView b) {
	        return a.getPrice() < b.getPrice() ? -1 : a.getPrice() == b.getPrice() ? 0 : 1 ;
	    }
	}
	
	class ItemNameComparator implements Comparator<ItemView> {
	    @Override
	    public int compare(ItemView a, ItemView b) {

	    	String s1 = a.getItemId().getProductId();
	    	String s2 = b.getItemId().getProductId();
	    	int sComp = s1.compareTo(s2);

	    	if (sComp != 0) {
	    		return sComp;
	    	} else {
	    		Integer i1 = new Integer(a.getPrice());
	    		Integer i2 = new Integer(b.getPrice());
	    		return i1.compareTo(i2);
	    	}
	    }
	}
	
	// Checks if id is an alphanumeric string without spaces
	public boolean checkId(String id) {
		Pattern p = Pattern.compile("[a-zA-Z0-9]+");
		Matcher m = p.matcher(id);
		return m.matches();
	}
	
	// View helpers -----------------------------------------------------
	
	public ItemView createItemView(ProductView product, SupplierClient client ){
		ItemView item = new ItemView();
		
		ItemIdView id  = new ItemIdView();
		id.setProductId(product.getId());
		id.setSupplierId(client.getWsName());
		
		item.setDesc(product.getDesc());
		item.setItemId(id);
		item.setPrice(product.getPrice());
		return item;
		
	}
    
	public ShoppingResultView createShoppingResultView(String id, Result res, int price) {
		ShoppingResultView view = new ShoppingResultView();
		view.setId(id);
		view.setResult(res);
		view.setTotalPrice(price);
		return view;
	}

	CartItemView createCartItemView(ProductView product, SupplierClient client, int qty){
		ItemView itemView = createItemView(product, client);
		CartItemView cartItemView = new CartItemView();
		cartItemView.setItem(itemView);
		cartItemView.setQuantity(qty);
		
		return cartItemView;
		
	}
	
	CartView createCartView(String cartId, ProductView product, SupplierClient client, int qty){
		
		CartItemView cartItemView = createCartItemView(product, client, qty);
		CartView cartView = new CartView();
		cartView.setCartId(cartId);
		cartView.getItems().add(cartItemView);
		return cartView;
		
	}
	
	
	// Exception helpers -----------------------------------------------------
	
	private void throwInvalidItemId(final String message) throws InvalidItemId_Exception {
		InvalidItemId faultInfo = new InvalidItemId();
		faultInfo.message = message;
		throw new InvalidItemId_Exception(message, faultInfo);
	}

	private void throwInvalidText(final String message) throws InvalidText_Exception{
		InvalidText faultInfo = new InvalidText();
		faultInfo.message = message;
		throw new InvalidText_Exception(message, faultInfo);
	}
	
	private void throwInvalidCartId(final String message) throws InvalidCartId_Exception{
		InvalidCartId faultInfo = new InvalidCartId();
		faultInfo.message = message;
		throw new InvalidCartId_Exception(message, faultInfo);
	}
	
	private void throwNotEnoughItems(final String message) throws NotEnoughItems_Exception{
		NotEnoughItems faultInfo = new NotEnoughItems();
		faultInfo.message = message;
		throw new NotEnoughItems_Exception(message, faultInfo);
	}

	private void throwInvalidQuantity(final String message) throws InvalidQuantity_Exception{
		InvalidQuantity faultInfo = new InvalidQuantity();
		faultInfo.message = message;
		throw new InvalidQuantity_Exception(message, faultInfo);
	}
	
	private void throwInvalidCreditCard(final String message) throws InvalidCreditCard_Exception{
		InvalidCreditCard faultInfo = new InvalidCreditCard();
		faultInfo.message = message;
		throw new InvalidCreditCard_Exception(message, faultInfo);
	}
	
	private void throwEmptyCart(final String message) throws EmptyCart_Exception{
		EmptyCart faultInfo = new EmptyCart();
		faultInfo.message = message;
		throw new EmptyCart_Exception(message, faultInfo);
	}
	
}
