package org.komparator.mediator.ws.it;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.komparator.mediator.ws.CartView;
import org.komparator.mediator.ws.InvalidCartId_Exception;
import org.komparator.mediator.ws.InvalidItemId_Exception;
import org.komparator.mediator.ws.InvalidQuantity_Exception;
import org.komparator.mediator.ws.ItemIdView;
import org.komparator.mediator.ws.NotEnoughItems_Exception;
import org.komparator.supplier.ws.BadProductId_Exception;
import org.komparator.supplier.ws.BadProduct_Exception;
import org.komparator.supplier.ws.ProductView;

public class AddToCartIT extends BaseIT {
	
    // tests
    // assertEquals(expected, actual);

    // public String ping(String x)
	
	@Before
	public void setUp() throws BadProductId_Exception, BadProduct_Exception, InvalidCartId_Exception, InvalidItemId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
		
		// clear remote service state before all tests

		// fill-in test products
		// (since getProduct is read-only the initialization below
		// can be done once for all tests in this suite)
		{
			ProductView product = new ProductView();
			product.setId("X1");
			product.setDesc("Baseball");
			product.setPrice(20);
			product.setQuantity(10);
			supplierClients.get(0).createProduct(product);
			
			
		}
		{
			ProductView product = new ProductView();
			product.setId("X2");
			product.setDesc("Baseball");
			product.setPrice(15);
			product.setQuantity(15);
			supplierClients.get(1).createProduct(product);
			
		}
		{
			ProductView product = new ProductView();
			product.setId("X3");
			product.setDesc("Baseball");
			product.setPrice(10);
			product.setQuantity(5);
			supplierClients.get(2).createProduct(product);

		}
		
	}
	
//	 Success tests
	
    @Test
    public void success() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X1");
		item.setSupplierId(supplierClients.get(0).getWsName());

    	mediatorClient.addToCart("Cart1", item, 8);
    	
    	List<CartView> carts = mediatorClient.listCarts();
    	assertEquals(1,carts.size());
    	assertEquals(carts.get(0).getCartId(),"Cart1" );
    	assertEquals(carts.get(0).getItems().get(0).getQuantity(), 8);
    	assertEquals(carts.get(0).getItems().get(0).getItem().getItemId().getProductId(), "X1");	
    }
    
    @Test
    public void successAddToCart3Carts() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X1");
		item.setSupplierId(supplierClients.get(0).getWsName());
    	mediatorClient.addToCart("Cart1",item, 8);
    	
    	ItemIdView item2 = new ItemIdView();
		item2.setProductId("X2");
		item2.setSupplierId(supplierClients.get(1).getWsName());
    	mediatorClient.addToCart("Cart2", item2, 10);
    	
    	ItemIdView item3 = new ItemIdView();
		item3.setProductId("X3");
		item3.setSupplierId(supplierClients.get(2).getWsName());
		mediatorClient.addToCart("Cart3", item3, 4);
		
    	List<CartView> carts = mediatorClient.listCarts();
    	assertEquals(3,carts.size());
    	assertEquals(carts.get(0).getCartId(),"Cart1" );
    	assertEquals(carts.get(1).getCartId(),"Cart2" );
    	assertEquals(carts.get(2).getCartId(),"Cart3" );
    	assertEquals(carts.get(0).getItems().get(0).getQuantity(), 8);
    	assertEquals(carts.get(1).getItems().get(0).getQuantity(), 10);
    	assertEquals(carts.get(2).getItems().get(0).getQuantity(), 4);
    	assertEquals(carts.get(0).getItems().get(0).getItem().getItemId().getProductId(), "X1");
    	assertEquals(carts.get(1).getItems().get(0).getItem().getItemId().getProductId(), "X2");
    	assertEquals(carts.get(2).getItems().get(0).getItem().getItemId().getProductId(), "X3"); 	
    }
    
    @Test
    public void successAddtoCartToTheSameCart() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 4);

    	mediatorClient.addToCart("Cart2", item, 10);
    	
    	List<CartView> carts = mediatorClient.listCarts();
    	assertEquals(1,carts.size());
    	assertEquals(carts.get(0).getCartId(),"Cart2" );
    	assertEquals(carts.get(0).getItems().get(0).getQuantity(), 14);
    	assertEquals(carts.get(0).getItems().get(0).getItem().getItemId().getProductId(), "X2");	
    }
    
    @Test
    public void successAddtoCartMoreThanOneItemDiffSuppliers() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 10);

    	ItemIdView item2 = new ItemIdView();
		item2.setProductId("X3");
		item2.setSupplierId(supplierClients.get(2).getWsName());
    	mediatorClient.addToCart("Cart2", item2, 4);
    	
    	List<CartView> carts = mediatorClient.listCarts();
    	assertEquals(1,carts.size());
    	assertEquals(carts.get(0).getCartId(),"Cart2" );
    	assertEquals(carts.get(0).getItems().get(0).getQuantity(), 10);
    	assertEquals(carts.get(0).getItems().get(1).getQuantity(), 4);
    	assertEquals(carts.get(0).getItems().get(0).getItem().getItemId().getProductId(), "X2");
    	assertEquals(carts.get(0).getItems().get(1).getItem().getItemId().getProductId(), "X3");
    }
   
//   Input Tests
    @Test(expected = InvalidCartId_Exception.class)
    public void addtoCartNullCartId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart(null, item, 4);
    }
    
    @Test(expected = InvalidCartId_Exception.class)
    public void addtoCartEmptyCartId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("", item, 4);
    }
    
    @Test(expected = InvalidCartId_Exception.class)
    public void addtoCartBlankCartId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("   ", item, 4);
    }
    
    @Test(expected = InvalidCartId_Exception.class)
    public void addtoCartNewLineCartId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("\n", item, 4);
    }
    
    @Test(expected = InvalidCartId_Exception.class)
    public void addtoCartTabCartId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("\t", item, 4);
    }
    
    @Test(expected = InvalidItemId_Exception.class)
    public void addtoCartNullItemId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", null, 4);
    }
    
    @Test(expected = InvalidItemId_Exception.class)
    public void addtoCartNewLineItemId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("\n");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 4);
    }
    
    @Test(expected = InvalidItemId_Exception.class)
    public void addtoCartEmptyItemId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 4);
    }
    
    @Test(expected = InvalidItemId_Exception.class)
    public void addtoCartBlankItemId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("   ");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 4);
    }
    
    @Test(expected = InvalidItemId_Exception.class)
    public void addtoCartTabItemId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("\t");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 4);
    }
    
//    Exceptions tests
    
    @Test(expected = NotEnoughItems_Exception.class)
    public void addtoCartNotEnoughItem() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 20);
    }
    
    @Test(expected = NotEnoughItems_Exception.class)
    public void addtoCartNotEnoughItemAdd2Times() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 4);

    	mediatorClient.addToCart("Cart2", item, 12);
    }
    
    @Test
    public void addToCartLimitNumberItem() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X1");
		item.setSupplierId(supplierClients.get(0).getWsName());

    	mediatorClient.addToCart("Cart1", item, 10);
    	
    	List<CartView> carts = mediatorClient.listCarts();
    	assertEquals(1,carts.size());
    	assertEquals(carts.get(0).getCartId(),"Cart1" );
    	assertEquals(carts.get(0).getItems().get(0).getQuantity(), 10);
    	assertEquals(carts.get(0).getItems().get(0).getItem().getItemId().getProductId(), "X1");	
    }
    
    @Test(expected = InvalidQuantity_Exception.class)
    public void addtoCartNegativeQuantity() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, -4);
    }
    
    @Test(expected = InvalidQuantity_Exception.class)
    public void addtoCartZeroItems() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X2");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 0);
    }
    
    @Test(expected = InvalidItemId_Exception.class)
    public void addtoCartInvalidItemId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X4");
		item.setSupplierId(supplierClients.get(1).getWsName());
		
    	mediatorClient.addToCart("Cart2", item, 4);
    }
    
    @Test(expected = InvalidItemId_Exception.class)
    public void addtoCartInvalidSupplierId() throws InvalidItemId_Exception, InvalidCartId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		
    	ItemIdView item = new ItemIdView();
		item.setProductId("X3");
		item.setSupplierId("A68_Supplier7");
		
    	mediatorClient.addToCart("Cart2", item, 4);
    }

    @After
    public void deleteCarts(){
    	mediatorClient.clear();
    }


}
