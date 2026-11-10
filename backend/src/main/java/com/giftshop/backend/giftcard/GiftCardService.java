package com.giftshop.backend.giftcard;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GiftCardService {
    
    private final GiftCardRepository repository;
    
    public GiftCardService(GiftCardRepository repository){
        this.repository = repository;
    }

    public List<GiftCard> getAll(){
        return repository.findAll();
    }

    public GiftCard getById(Long id){
        return repository.findById(id)
                .orElseThrow(() -> new GiftCardNotFoundException(id));
    }
}
