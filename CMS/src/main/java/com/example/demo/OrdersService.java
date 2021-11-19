package com.example.demo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrdersService {
	
	
    @Autowired 
    private MenuDAO mdao;
	    @Autowired
	    private OrdersRepository repo;
	    @Autowired
	    private CustomerDAO cdao;

	    @Autowired 
	    private WalletDAO wdao;
	    @Autowired
	    private OrdersDAO dao;
	    @Autowired
	    private JavaMailSender javaMailSender;
	   		    
	    public List<Orders> showVendorPendingOrders(int venId) {
			return dao.showVendorPendingOrders(venId);
		}
		public List<Orders> showVendorOrders(int venId) {
			return dao.showVendorOrders(venId);
		}
		public List<Orders> showCustomerOrders(int custId) {
			return dao.showCustomerOrders(custId);
		}
		public List<Orders> showCustomerPendingOrders(int custId) {
			return dao.showCustomerPendingOrders(custId);
		}
		public List<Orders> showOrders() {
	        return repo.findAll();
	    }
		public Orders get(Integer ord_id) {
			return repo.findById(ord_id).get();
		}
		
		public String placeOrder(Orders order) {
	    	order.setOrd_id(dao.generateordid());
	    
	        Menu menu = mdao.searchMenu(order.getMen_id());
	        String email=cdao.getMailId(order.getCus_id());
	       Wallet wallet = wdao.showCustomerWallet(order.getCus_id(), order.getWal_source());
	        double balance = wallet.getWal_amount();
	        double billAmount = order.getOrd_quantity()*menu.getMen_price();
	        System.out.println(balance);
	        System.out.println(billAmount);
	        if (balance-billAmount > 0) {
	            order.setOrd_status("PENDING");
	            order.setOrd_billamount(order.getOrd_quantity()*menu.getMen_price());
	            repo.save(order);
	            wdao.updateWallet(order.getCus_id(), order.getWal_source(), billAmount);
	            SimpleMailMessage msg = new SimpleMailMessage();
               msg.setTo(email);

		        msg.setSubject("Your Order will be delivered within 10 mins");
              
               msg.setText("Hello there! \n Customer ID : " + Integer.toString(order.getCus_id()) + "\nStatus:" + order.getOrd_status() + 
               		"\n Bill Amount : " + Double.toString(order.getOrd_billamount()) + 
               		"\n Order Quantity : " +Integer.toString(order.getOrd_quantity()));
	        
               javaMailSender.send(msg);
			               
	            return "Order Placed Successfully...and Amount Debited";
	        }
	        return "Insufficient Funds...";
	    }
		public String acceptOrRejectOrder(int ordId,int venId,String status) {
			Orders orders = dao.searchByOrderId(ordId);
		    
			int vid = orders.getVen_id();
			int cid = orders.getCus_id();
			String walType = orders.getWal_source();
			double billAmount = orders.getOrd_billamount();
			if (vid!=venId) {
				return "You are unauthorized vendor...";
			} 
			if (status.toUpperCase().equals("YES")) {
				acceptrejectmail(orders.getOrd_id(),"ACCEPTED");
				return dao.updateStatus(ordId,"ACCEPTED");								
			} else {
				dao.updateStatus(ordId, "DENIED");
				wdao.refundWallet(cid, walType, billAmount);
				acceptrejectmail(orders.getOrd_id(),"DENIED");
				return "Order Rejected and Amount Refunded...";
			}
			
		}
		
		public void acceptrejectmail(int ordId,String sts) {
			Orders order = dao.searchByOrderId(ordId);
			String email =cdao.getMailId(order.getCus_id());
	        Menu menu = mdao.searchMenu(order.getMen_id());
	        Wallet wallet = wdao.showCustomerWallet(order.getCus_id(), order.getWal_source());
			SimpleMailMessage msg = new SimpleMailMessage();
	        msg.setTo(email);
	        msg.setSubject("Order " + sts);
          
            msg.setText("Hello there! \nCustomer ID : " + Integer.toString(order.getCus_id()) + "\nMenu Item: " + menu.getMen_item() +
            		"\nBill Amount : " + Double.toString(order.getOrd_billamount()) + "\nWallet Chosen: " + wallet.getWal_source() +
            		"\nStatus:" + sts);
            javaMailSender.send(msg);			
		}
}


