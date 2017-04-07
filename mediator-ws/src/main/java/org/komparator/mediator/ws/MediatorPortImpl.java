package org.komparator.mediator.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jws.WebService;

import org.komparator.supplier.ws.BadText_Exception;
import org.komparator.supplier.ws.BadProductId_Exception;
import org.komparator.supplier.ws.ProductView;
import org.komparator.supplier.ws.PurchaseView;
import org.komparator.supplier.ws.cli.SupplierClient;
import org.komparator.supplier.ws.cli.SupplierClientException;

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

	public MediatorPortImpl(MediatorEndpointManager endpointManager) {
		this.endpointManager = endpointManager;
	}

	// Main operations -------------------------------------------------------
	
	@Override
	public List<ItemView> getItems(String productId) throws InvalidItemId_Exception {
		
		List<SupplierClient> supClientList = getSupplierClients(getSuppliers());
		
		System.out.println("FOUND THIS NUMBER OF SUPPLIERS : " + supClientList.size());
    	
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
    	
    	if(itemList.isEmpty()){
    		throwInvalidItemId("Product does not exist in any Supplier.");
    	}
    	
    	Collections.sort(itemList,new ItemPriceComparator() );
		return itemList;
	}
	
	@Override
	public List<ItemView> searchItems(String descText) throws InvalidText_Exception {
		//TODO testar input
		List<ItemView> itemList = new ArrayList<ItemView>();
		
		for (SupplierClient supC : getSupplierClients(getSuppliers())) {
			try {
				for(ProductView prod : supC.searchProducts(descText)) {
					itemList.add(createItemView(prod, supC));
				}
			} catch (BadText_Exception e) {
				throwInvalidText("Invalid description");
			}
		}
		
		Collections.sort(itemList,new ItemNameComparator() );
		return itemList;
	}

	@Override
	public void addToCart(String cartId, ItemIdView itemId, int itemQty) throws InvalidCartId_Exception,
	InvalidItemId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {		
		
		if( cartId==null || cartId.trim().equals("") ) throwInvalidCartId("Cart ID is incorrect, failed.");
				
		if(itemId == null || itemId.getProductId().trim().equals("") || getItems(itemId.getProductId())==null)  throwInvalidItemId("Item ID is incorrect, failed.");
		
		if( itemQty < 0 ) throwInvalidQuantity("Quantity is invalid, failed.");

		int supQuantity=0;
		try {
			ProductView product = getSupplierClient(itemId.getSupplierId()).getProduct(itemId.getProductId());
			supQuantity = product.getQuantity();
		} catch (BadProductId_Exception e) {
			throwInvalidItemId("Item ID is incorrect, failed.");
		}
		
		if( supQuantity < itemQty) throwNotEnoughItems("Not enough items, failed.");

		
		for(CartView c : carts){
			
			if(c.getCartId().equals(cartId)){
				
				for(CartItemView civ : c.getItems()){
					
					if(civ.getItem().getItemId().equals(itemId)){
						int qty = civ.getQuantity() + itemQty;
						if(qty > supQuantity) throwNotEnoughItems("Not enough items, failed.");
						civ.setQuantity(qty);
						return;
					}
				}
				
				c.getItems().add(createCartItemView(itemId, itemQty));
				return;
			}
		}
		
		if(itemQty > supQuantity) throwNotEnoughItems("Not enough items, failed.");
		
		carts.add(createCartView(cartId, itemId, itemQty));

	}
	
	@Override
	public ShoppingResultView buyCart(String cartId, String creditCardNr)
			throws EmptyCart_Exception, InvalidCartId_Exception, InvalidCreditCard_Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
    
	// Auxiliary operations --------------------------------------------------	
	
	
	public Collection<UDDIRecord> getSuppliers(){
		Collection<UDDIRecord> suppliers;
    	//É para fazer try catch ou throws? !!!!!!!!!!!!!!!!!!! TODO
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
    			System.out.println("");
    		}
    	}
    	return supClientList;
    	
	}

	public SupplierClient getSupplierClient(String supplier){
		for(SupplierClient sc : getSupplierClients(getSuppliers())){
			if(sc.getWsName().equals(supplier)){
				return sc;
			}
		}
		return null;
	
	}
	
    public String ping(String arg0){
    	
    	Collection<UDDIRecord> suppliers=getSuppliers();
    	List<SupplierClient> supClientList = getSupplierClients(suppliers);
    	String result = "";
    	int i = 0;
    	for(SupplierClient client : supClientList){
    		result+=client.ping("Supplier Client " + i)+ "\n";
    		i++;
    	}
    	
    	return result;
    	
    }

	@Override
	public void clear() {
		
		List<SupplierClient> supClientList = getSupplierClients(getSuppliers());
		for(SupplierClient client : supClientList){
			client.clear();
		}
		
	}

	@Override
	public List<CartView> listCarts() {
		return carts;
	}
		

	@Override
	public List<ShoppingResultView> shopHistory() {
		return shoppingResults;
	}

	
	// View helpers -----------------------------------------------------
	
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

	CartItemView createCartItemView(ItemIdView id, int qty){
		ItemView itemView = new ItemView();
		itemView.setItemId(id);
		
		CartItemView cartItemView = new CartItemView();
		cartItemView.setItem(itemView);
		cartItemView.setQuantity(qty);
		
		return cartItemView;
		
	}
	
	CartView createCartView(String cartId, ItemIdView id, int qty){
		
		CartItemView cartItemView = createCartItemView(id, qty);
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
	
	
	
}
