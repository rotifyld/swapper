# swapper

Swapper is a synchronizing set implementing only one blocking method: ```swap``` working as follows: (see snippet below)
 - wait until swapper contains all the elements in ```removed``` collection;
 - do atomic swap (remove all the elements from ```removed```, and then add all elements from ```added``` collections).

Swapper is able to work as: semaphore table, (cyclic) barrier, and many more depending on users creativity. 

Deployed with 2 example programs implemented using ```Swapper``` solving:
 - [producer-consumer problem](https://en.wikipedia.org/wiki/Producer%E2%80%93consumer_problem),
 - [readers–writers problem](https://en.wikipedia.org/wiki/Readers%E2%80%93writers_problem).
```
package swapper;

import java.util.Collection;
...

public class Swapper<E> {

    public Swapper() {
        ...
    }

    public void swap(Collection<E> removed, Collection<E> added) throws InterruptedException {
        ...
    }

    ...

}
```
