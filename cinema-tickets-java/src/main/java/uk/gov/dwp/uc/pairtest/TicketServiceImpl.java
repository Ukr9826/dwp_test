package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import thirdparty.seatbooking.SeatReservationServiceImpl;

public class TicketServiceImpl implements TicketService {
    /**
     * Since infant price is zero it is not defined, the rest of the constants are used to define ticket prices
     */
    private static final int CHILD_PRICE = 15;
    private static final int ADULT_PRICE = 25;
    // Objects for calling methods to reserve seats and make payments for a ticket
    SeatReservationServiceImpl seatReservationService = new SeatReservationServiceImpl();
    TicketPaymentServiceImpl ticketPaymentService = new TicketPaymentServiceImpl();

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (accountId == null || accountId < 0) {
            throw new InvalidPurchaseException("Invalid account");
        }
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("No ticket requests provided");
        }
        int total_tickets = 0; // to add the total number of tickets
        int total_seats = 0; // to add total number of seats since infants doesn't need a seat
        boolean hasAdult = false; // check whether adult ticket is there in the request made

        for (TicketTypeRequest req : ticketTypeRequests) {
            if (req.getNoOfTickets()< 0){ //Since integer entered can also be negative
                throw new InvalidPurchaseException("The number of tickets cannot be negative");
            }
            if (req.getTicketType() == TicketTypeRequest.Type.ADULT) {
                hasAdult = true;
            }
            total_tickets += req.getNoOfTickets();
            if (req.getTicketType() != TicketTypeRequest.Type.INFANT) { //Infants don't need a seat
                total_seats += req.getNoOfTickets();
            }
        }
        if (!hasAdult) {
            throw new InvalidPurchaseException("At least one adult ticket must be purchased in order to buy child or infant tickets");
        }
        if (total_tickets > 25) {
            throw new InvalidPurchaseException("The total number of tickets must not exceed 25");
        }
        int total_amount = calculateTotalPrice(ticketTypeRequests);
        ticketPaymentService.makePayment(accountId, total_amount); // Assuming if payment fails the code will throw an exception so no seats are allocated
        seatReservationService.reserveSeat(accountId, total_seats);

    }

    private int calculateTotalPrice( TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        int total_amount = 0;
        for (TicketTypeRequest req: ticketTypeRequests){
            switch (req.getTicketType()){
                case ADULT:
                    total_amount += req.getNoOfTickets()*ADULT_PRICE;
                    break;
                case CHILD:
                    total_amount += req.getNoOfTickets()*CHILD_PRICE;
                    break;
                case INFANT: // no calculation is needed since infant price is zero
                    break;
                default: // as a defensive method to encounter any unknown ticket type
                    throw new InvalidPurchaseException("Unknown ticket type "+ req.getTicketType());
            }

        }
        return total_amount;
    }
}
