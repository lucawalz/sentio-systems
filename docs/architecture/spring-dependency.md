## Spring Dependency Injection: Modularity and Testability

Spring’s dependency injection (DI) improves **modularity** by wiring components externally (via the Spring container) instead of creating dependencies manually using `new`. Components declare what they need, and Spring provides the correct implementation at runtime. This reduces coupling and makes implementations replaceable.

### Modularity through constructor injection

In the backend, dependencies are injected into components (e.g., controllers and services) via constructor injection. For example, the `DeviceController` receives its collaborators as injected dependencies:

```java
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceLocationService deviceLocationService;
    private final RateLimitService rateLimitService;

    // ...
}
```
### Testability via mocking injected dependencies

Because dependencies are injected, they can be replaced in tests with mocks or fakes.  
For example, `ContactServiceTest` injects a mocked `ResendEmailService` so the service can be tested without sending real emails:

```java
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ResendEmailService emailService;

    @InjectMocks
    private ContactService contactService;

    @Test
    void shouldSendEmailWithAllFields() {
        ContactRequest request = new ContactRequest();
        request.setReference("Product Inquiry");
        request.setMail("john.doe@example.com");

        contactService.sendContactMail(request);

        verify(emailService).sendEmail(
                eq("team@syslabs.dev"),
                eq("New contact message: Product Inquiry"),
                anyString(),
                eq("john.doe@example.com")
        );
    }
}
```

---