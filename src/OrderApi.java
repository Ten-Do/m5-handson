import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderApi {
    private final OrderService service;

    public OrderApi(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderDto> list() {
        return service.listAll().stream().map(OrderDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> get(@PathVariable("id") String id) {
        return service.findById(id)
                .map(o -> ResponseEntity.ok(OrderDto.from(o)))
                .orElseGet(ResponseEntity::notFound);
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(@RequestBody CreateOrderRequest req) {
        if (req == null || req.customerId() == null || req.customerId().isBlank()
                || req.amount() == null || req.amount().signum() <= 0) {
            return ResponseEntity.badRequest();
        }
        Order o = service.create(req.customerId(), req.amount());
        return ResponseEntity.created(URI.create("/orders/" + o.getId()), OrderDto.from(o));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable("id") String id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound();
        }
        if (!service.cancel(id)) {
            return ResponseEntity.status(409);
        }
        return ResponseEntity.noContent();
    }
}
