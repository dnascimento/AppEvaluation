package inesc.serverAPI;

import inesc.restAPI.AddressBookProtos;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/person")
public class AddressBookService {
    @GET
    @Produces("application/x-protobuf")
    public AddressBookProtos.Person gePerson() {
        return AddressBookProtos.Person.newBuilder()
                                       .setId(1)
                                       .setName("Sam")
                                       .setEmail("ze@ze.com")
                                       .addPhone(AddressBookProtos.Person.PhoneNumber.newBuilder()
                                                                                     .setNumber("412")
                                                                                     .setType(AddressBookProtos.Person.PhoneType.HOME)
                                                                                     .build())
                                       .build();
    }

    @POST
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public AddressBookProtos.Person reflect(AddressBookProtos.Person person) {
        return person;
    }
}
