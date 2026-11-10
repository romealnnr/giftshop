package com.giftshop.backend.giftcard;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping ("api/giftcards")
public class GiftCardController {

    private final GiftCardService service;

    public GiftCardController(GiftCardService service){
        this.service = service;
    }

    @GetMapping 
    public  List<GiftCard> getAll(){
        return  service.getAll();
    }

    @GetMapping ("/{id}")
    public GiftCard getById(@PathVariable Long id){
        return  service.getById(id);
    }
}
